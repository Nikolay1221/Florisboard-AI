import os
import tensorflow as tf
from transformers import TFAutoModel, AutoTokenizer

MODEL_NAME = "cointegrated/rubert-tiny2"
OUTPUT_DIR = "sbert_tflite"

def export_model():
    print(f"Loading model {MODEL_NAME}...")
    tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
    model = TFAutoModel.from_pretrained(MODEL_NAME)

    os.makedirs(OUTPUT_DIR, exist_ok=True)
    
    # Save vocabulary
    vocab_file = os.path.join(OUTPUT_DIR, "vocab.txt")
    with open(vocab_file, "w", encoding="utf-8") as f:
        for token, index in sorted(tokenizer.get_vocab().items(), key=lambda x: x[1]):
            f.write(f"{token}\n")
    print(f"Saved vocabulary to {vocab_file}")

    # We need a concrete function for TFLite conversion
    # rubert-tiny2 takes input_ids, attention_mask, token_type_ids
    # Max sequence length could be 64 for swipe suggestions
    MAX_SEQ_LEN = 64
    
    # Create a wrapper model that outputs the sentence embedding
    class SbertWrapper(tf.keras.Model):
        def __init__(self, base_model):
            super().__init__()
            self.base_model = base_model
            
        @tf.function(input_signature=[
            tf.TensorSpec(shape=[None, None], dtype=tf.int32, name='input_ids'),
            tf.TensorSpec(shape=[None, None], dtype=tf.int32, name='attention_mask'),
            tf.TensorSpec(shape=[None, None], dtype=tf.int32, name='token_type_ids')
        ])
        def call(self, input_ids, attention_mask, token_type_ids):
            outputs = self.base_model(
                input_ids=input_ids, 
                attention_mask=attention_mask, 
                token_type_ids=token_type_ids
            )
            # Use [CLS] token embedding as sentence embedding
            # For rubert-tiny2, CLS is the first token
            cls_embedding = outputs.last_hidden_state[:, 0, :]
            
            # Normalize embedding (L2 normalization)
            normalized = tf.nn.l2_normalize(cls_embedding, axis=1)
            return {"embedding": normalized}

    wrapper = SbertWrapper(model)
    
    # Convert to TFLite
    print("Converting to TFLite...")
    converter = tf.lite.TFLiteConverter.from_keras_model(wrapper)
    
    # Enable dynamic range quantization for size reduction
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    
    # Important for models with custom ops or control flow
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.TFLITE_BUILTINS, 
        tf.lite.OpsSet.SELECT_TF_OPS
    ]
    
    tflite_model = converter.convert()
    
    model_file = os.path.join(OUTPUT_DIR, "rubert_tiny2.tflite")
    with open(model_file, "wb") as f:
        f.write(tflite_model)
        
    print(f"Saved TFLite model to {model_file}")
    print(f"Size: {len(tflite_model) / (1024 * 1024):.2f} MB")

if __name__ == "__main__":
    export_model()
