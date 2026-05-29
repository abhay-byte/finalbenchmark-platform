# AI Benchmark — NPU Acceleration Guide by Chipset

**Date:** 2026-05-28  
**Scope:** How to run AI benchmarks on NPU for each mobile chipset family  
**Current Status:** Only NNAPI fallback used; no vendor-specific NPU integration

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                   Your Benchmark App                     │
├─────────────────────────────────────────────────────────┤
│               TFLite Interpreter (Java/Kotlin)           │
├──────────┬──────────┬──────────┬──────────┬─────────────┤
│ NNAPI    │ QNN      │ Neuron   │ ONE      │ XNNPACK     │
│ Delegate │ Delegate │ Delegate │ Runtime  │ (CPU)       │
│ (Stock)  │ (QC)     │ (MTK)    │ (Samsung)│             │
├──────────┼──────────┼──────────┼──────────┼─────────────┤
│ Android  │ Qualcomm │ MediaTek │ Samsung  │ ARM CPU     │
│ NN HAL   │ HTP NPU  │ APU NPU  │ TriX NPU │             │
└──────────┴──────────┴──────────┴──────────┴─────────────┘
```

**Key insight:** All four chipset vendors route NPU access through different APIs — there is NO single "NPU delegate" that works everywhere. NNAPI is the closest universal path, but it doesn't always map to the NPU on every device.

---

## 1. Qualcomm Snapdragon (Adreno GPU + Hexagon NPU/HTP)

### SDK: Qualcomm QNN (Qualcomm Neural Network)
- **Successor to:** SNPE (Snapdragon Neural Processing Engine)
- **NPU name:** Hexagon Tensor Processor (HTP) or Hexagon DSP
- **Download:** [Qualcomm Developer Network](https://developer.qualcomm.com/software/qualcomm-neural-processing-sdk)
- **License:** Proprietary, requires Qualcomm account

### Integration Approaches

#### A. TFLite → QNN Delegate (Recommended)
```kotlin
// 1. Add QNN delegate .aar to your project
// Download from Qualcomm Developer Network

// 2. Load QNN delegate dynamically
val qnnDelegate = QnnDelegate(
    QnnDelegate.Options().apply {
        setBackendType("HTP")  // HTP = NPU, DSP = Hexagon DSP
        setProfilingLevel(QnnDelegate.ProfilingLevel.BASIC)
    }
)

val options = Interpreter.Options().apply {
    addDelegate(qnnDelegate)
}
val interpreter = Interpreter(modelFile, options)
// NPU inference now active via QNN
interpreter.close()
qnnDelegate.close()
```

**Key points:**
- QNN HTP backend = actual NPU acceleration (fastest)
- QNN DSP backend = Hexagon DSP (good, but NPU is 2-5× faster)
- Models must be INT8 quantized for optimal NPU performance
- FP16 works on HTP but not all ops supported
- FP32 only works on DSP, not HTP

#### B. NNAPI → QNN HTP (via Android NN HAL)
```kotlin
// On Qualcomm devices with QNN NN HAL driver installed,
// NNAPI automatically routes to QNN HTP
val nnApiDelegate = NnApiDelegate(
    NnApiDelegate.Options().apply {
        setAcceleratorName("qti-default")     // Force Qualcomm backend
        setAllowFp16(true)
        setExecutionPreference(NnApiDelegate.EXECUTION_PREFERENCE_FAST_SINGLE_ANSWER)
    }
)
```

**Key points:**
- Works out of the box with TFLite NNAPI delegate
- Requires device to have QNN NN HAL driver (most Snapdragon 8 Gen 1+ phones do)
- No extra SDK needed — uses stock Android NNAPI
- Good for: MobileNet, EfficientDet, MobileBERT
- May not work for: complex LSTM models (DTLN, Whisper)

### Detection
```kotlin
fun isSnapdragonDevice(): Boolean {
    return Build.HARDWARE.contains("qcom") || 
           Build.SOC_MANUFACTURER?.contains("Qualcomm") == true
}
```

---

## 2. MediaTek Dimensity (APU NPU)

### SDK: MediaTek NeuroPilot
- **NPU name:** APU (AI Processing Unit)
- **TFLite delegate:** [mediatek-neuropilot/tflite-neuron-delegate](https://github.com/mediatek-neuropilot/tflite-neuron-delegate)
- **License:** Apache 2.0 (open source)
- **Build system:** Bazel

### Integration

```kotlin
// 1. Build the delegate .aar: bazel build //neuron/java:tensorflow-lite-neuron
// 2. Add to your project as a dependency

// 3. Create Neuron delegate
val neuronDelegate = NeuronDelegate()

val options = Interpreter.Options().apply {
    addDelegate(neuronDelegate)
}
val interpreter = Interpreter(modelFile, options)
```

**Key points:**
- Neuron delegate is the most direct path to APU NPU
- Supports INT8 quantized models (required for APU)
- FP16 models also supported but with lower performance
- Delegate is open-source but requires Bazel build system
- MediaTek Dimensity 9000+ phones have the strongest APU

### NNAPI Fallback
```kotlin
// MediaTek ships an NNAPI NN HAL driver that routes to APU
val nnApiDelegate = NnApiDelegate(
    NnApiDelegate.Options().apply {
        setAcceleratorName("mtk-APU")
        setAllowFp16(true)
    }
)
```

### Detection
```kotlin
fun isMediaTekDevice(): Boolean {
    return Build.HARDWARE.contains("mt") || 
           Build.BOARD.contains("mt") ||
           Build.HARDWARE.contains("mediatek")
}
```

---

## 3. Google Tensor / Pixel (Edge TPU)

### Architecture
- **NPU name:** Google Tensor Processing Unit (TPU) / Edge TPU
- **Access path:** Only through Android NNAPI
- **SDK:** No separate SDK — uses stock Android NNAPI

### Integration — NNAPI Only

```kotlin
// Google Tensor uses Android NNAPI with Google's custom NN HAL driver
// The TPU is NOT exposed as a separate delegate
// All NPU access goes through NNAPI

val nnApiDelegate = NnApiDelegate(
    NnApiDelegate.Options().apply {
        setAcceleratorName("google-edgetpu")  // Optional: force TPU
        setAllowFp16(true)
        setExecutionPreference(
            NnApiDelegate.EXECUTION_PREFERENCE_FAST_SINGLE_ANSWER
        )
        // For large models, use SUSTAINED_SPEED to avoid thermal throttling
        setExecutionPriority(NnApiDelegate.EXECUTION_PRIORITY_HIGH)
    }
)

val options = Interpreter.Options().apply {
    addDelegate(nnApiDelegate)
}
val interpreter = Interpreter(modelFile, options)
```

**Key points:**
- No external SDK needed — built into Android
- NNAPI version 1.3+ required for Edge TPU acceleration
- FP16 models recommended for optimal TPU performance
- INT8 models also work well
- Some operations fall back to CPU if TPU doesn't support them
- Pixel 6-9 all use this same path

### Detection
```kotlin
fun isPixelDevice(): Boolean {
    return Build.MANUFACTURER.equals("Google", ignoreCase = true)
}
```

---

## 4. Samsung Exynos (NPU)

### SDK: Samsung ONE Runtime (On-device Neural Engine)
- **NPU name:** Samsung NPU (Exynos NPU) via TriX backend
- **Repository:** [github.com/samsung/one](https://github.com/samsung/one)
- **License:** Apache 2.0 (open source)

### Integration Approaches

#### A. Samsung ONE Runtime (Direct NPU Access)
```java
// Samsung ONE Runtime with NPU backend
import com.samsung.onert.Session;
import com.samsung.onert.Tensor;

// npu, acl_cl, xnnpack, cpu, ruy, trix backends available
Session session = new Session("/sdcard/model_package/", "npu");
session.prepare();

Tensor[] inputs = new Tensor[session.getInputSize()];
for (int i = 0; i < session.getInputSize(); ++i) {
    TensorInfo ti = session.getInputTensorInfo(i);
    inputs[i] = new Tensor(ti);
}
session.setInputs(inputs);

Tensor[] outputs = new Tensor[session.getOutputSize()];
// ... similar allocation for outputs
session.setOutputs(outputs);

// Fill input data
inputs[0].buffer().put(inputByteBuffer);

session.run();

// Read output data
outputs[0].buffer().get(outputArray);

session.close();
```

**Key points:**
- Most direct NPU access on Samsung devices
- Requires models packaged in NNPKG format (not raw .tflite)
- `trix` backend = Samsung NPU (fastest)
- `acl_cl` backend = GPU via OpenCL
- `acl_neon` backend = CPU via ARM Compute Library
- `xnnpack` backend = CPU via XNNPACK
- Backend selection: `npu`, `acl_cl`, `xnnpack`, `cpu`, `ruy`, `trix`

#### B. ONE Runtime TFLite Runner (Model Compatibility)
```bash
# ONE Runtime includes a TFLite runner that can execute .tflite models on NPU
# This wraps the TFLite model for ONE Runtime's backends

# Convert TFLite → NNPKG for NPU execution
tools/nnpackage_tool/model2nnpkg/model2nnpkg.sh \
    -m your_model.tflite \
    -p output_package

# Run with NPU backend
./Product/out/bin/tflite_run \
    --tflite your_model.tflite \
    --backend npu
```

#### C. NNAPI → Samsung NPU (via Android NN HAL)
```kotlin
// Samsung ships custom NNAPI NN HAL driver
val nnApiDelegate = NnApiDelegate(
    NnApiDelegate.Options().apply {
        setAcceleratorName("samsung-exynos")
        setAllowFp16(true)
        setExecutionPreference(
            NnApiDelegate.EXECUTION_PREFERENCE_FAST_SINGLE_ANSWER
        )
    }
)
```

**Key points:**
- Works with existing TFLite NNAPI delegate
- Good for quick integration
- May not fully utilize NPU — ONE Runtime is faster
- TriX backend supports Q8 and Q16 quantized types

### Detection
```kotlin
fun isSamsungExynosDevice(): Boolean {
    return (Build.MANUFACTURER.equals("Samsung", ignoreCase = true) &&
            !Build.HARDWARE.contains("qcom"))
}
```

---

## Practical Implementation Strategy

### Recommended Detection & Routing

```kotlin
enum class NpuBackend {
    QNN_HTP,      // Qualcomm Hexagon NPU
    NEURON_APU,   // MediaTek APU
    NNAPI_TPU,    // Google Tensor TPU (via NNAPI)
    ONE_TRIX,     // Samsung Exynos NPU
    NNAPI,        // Generic NNAPI fallback
    GPU,          // TFLite GPU delegate
    CPU           // XNNPACK fallback
}

fun selectNpuBackend(): NpuBackend {
    return when {
        isSnapdragonDevice() -> {
            if (hasQnnLibrary()) NpuBackend.QNN_HTP
            else NpuBackend.NNAPI  // QNN NN HAL via NNAPI
        }
        isMediaTekDevice() -> {
            if (hasNeuronDelegate()) NpuBackend.NEURON_APU
            else NpuBackend.NNAPI
        }
        isPixelDevice() -> NpuBackend.NNAPI_TPU  // Only via NNAPI
        isSamsungExynosDevice() -> {
            if (hasOneRuntime()) NpuBackend.ONE_TRIX
            else NpuBackend.NNAPI
        }
        else -> NpuBackend.NNAPI
    }
}

fun createNpuInterpreter(modelFile: File, backend: NpuBackend): Interpreter {
    val options = Interpreter.Options()
    
    when (backend) {
        NpuBackend.QNN_HTP -> {
            options.addDelegate(QnnDelegate(
                QnnDelegate.Options().apply { setBackendType("HTP") }
            ))
        }
        NpuBackend.NEURON_APU -> {
            options.addDelegate(NeuronDelegate())
        }
        NpuBackend.NNAPI_TPU, NpuBackend.NNAPI -> {
            options.addDelegate(NnApiDelegate(
                NnApiDelegate.Options().apply {
                    setAllowFp16(true)
                    setExecutionPreference(
                        NnApiDelegate.EXECUTION_PREFERENCE_FAST_SINGLE_ANSWER
                    )
                }
            ))
        }
        NpuBackend.ONE_TRIX -> {
            // Samsung ONE Runtime — use Session API, not TFLite Interpreter
            // See Samsung section above
        }
        NpuBackend.GPU -> {
            options.addDelegate(GpuDelegate())
        }
        NpuBackend.CPU -> {
            options.setUseXNNPACK(true)
            options.setNumThreads(Runtime.getRuntime().availableProcessors())
        }
    }
    
    return Interpreter(modelFile, options)
}
```

### Common Gotchas

| Issue | Cause | Fix |
|-------|-------|-----|
| NNAPI falls back to CPU silently | Model has unsupported ops | Check `nnApiDelegate.getSupportedOperationsForDevices()` |
| QNN delegate not found | .aar not included or wrong arch | QNN ships separate .aar per ABI |
| Neuron delegate crashes | Model not INT8 quantized | Quantize model before deploying |
| Samsung ONE rejects model | Wrong NNPKG format | Use `model2nnpkg.sh` to package |
| Pixel TPU slower than CPU | Model too small (overhead > benefit) | Only use TPU for models > 2M ops |
| FP32 model on NPU fails | NPU requires INT8 or FP16 | Quantize model or request `setAllowFp16(true)` |

### Model Quantization Requirements

```
Chipset        | Optimal Format | Tool
───────────────┼────────────────┼──────────────────
Snapdragon QNN | INT8           | QNN Converter Tool
MediaTek APU   | INT8           | TFLite Converter
Google TPU     | FP16 or INT8   | TFLite Converter
Samsung NPU    | Q8 / Q16       | ONE Runtime tools
```

---

## TFLite Performance Compared: NPU vs GPU vs CPU

Based on published benchmarks (MobileNet V3, 224×224, Adreno 750 / SD8 Gen3):

| Backend      | Latency (ms) | Power (W) | Notes                    |
|-------------|-------------|-----------|--------------------------|
| QNN HTP     | 0.8-1.2     | 0.3-0.5   | Fastest, lowest power    |
| NNAPI (QNN) | 1.5-2.5     | 0.8-1.2   | Good, uses HTP via HAL   |
| GPU Delegate| 2.0-3.0     | 1.5-2.0   | Good perf, high power    |
| XNNPACK CPU | 4.0-6.0     | 2.0-2.5   | Slowest, highest power   |

**Conclusion:** NPU delivers 3-5× better perf/W than CPU. Any benchmark that doesn't exercise the NPU is measuring the wrong thing.

---

## References

- **Qualcomm QNN SDK:** https://developer.qualcomm.com/software/qualcomm-neural-processing-sdk
- **MediaTek Neuron Delegate:** https://github.com/mediatek-neuropilot/tflite-neuron-delegate
- **Samsung ONE Runtime:** https://github.com/samsung/one
- **Android NNAPI:** https://developer.android.com/ndk/guides/neuralnetworks
- **TFLite GPU Delegate:** https://github.com/tensorflow/tensorflow/blob/master/tensorflow/lite/g3doc/android/delegates/gpu.md
- **TFLite NNAPI Delegate:** https://github.com/tensorflow/tensorflow/blob/master/tensorflow/lite/g3doc/android/delegates/nnapi.md
