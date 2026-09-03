package com.example.myscreentime.fragments.wellbeingscreen

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ActivityClassifier(
    context: Context,
    private val onResult: (String, Float) -> Unit
) : SensorEventListener {

    private val labels = listOf(
        "Walking", "Walking Upstairs", "Walking Downstairs",
        "Sitting", "Standing", "Laying"
    )

    private val windowSize = 128
    private val bodyAcc = mutableListOf<FloatArray>()
    private val bodyGyro = mutableListOf<FloatArray>()
    private val totalAcc = mutableListOf<FloatArray>()

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val interpreter: Interpreter

    init {
        val modelFile = context.assets.openFd("model_final.tflite")
        val inputStream = FileInputStream(modelFile.fileDescriptor)
        val modelBuffer: MappedByteBuffer = inputStream.channel.map(
            FileChannel.MapMode.READ_ONLY,
            modelFile.startOffset,
            modelFile.declaredLength
        )
        interpreter = Interpreter(modelBuffer)
    }

    fun start() {
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
            SensorManager.SENSOR_DELAY_GAME
        )
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
            SensorManager.SENSOR_DELAY_GAME
        )
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun close() {
        stop()
        interpreter.close()
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                if (bodyAcc.size < windowSize) {
                    bodyAcc.add(floatArrayOf(event.values[0], event.values[1], event.values[2]))
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                if (bodyGyro.size < windowSize) {
                    bodyGyro.add(floatArrayOf(event.values[0], event.values[1], event.values[2]))
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                if (totalAcc.size < windowSize) {
                    totalAcc.add(floatArrayOf(event.values[0], event.values[1], event.values[2]))
                }
            }
        }

        if (bodyAcc.size == windowSize && bodyGyro.size == windowSize && totalAcc.size == windowSize) {
            runInference()
            bodyAcc.clear()
            bodyGyro.clear()
            totalAcc.clear()
        }
    }

    private fun runInference() {
        val input = Array(1) { Array(windowSize) { FloatArray(9) } }

        for (i in 0 until windowSize) {
            input[0][i][0] = bodyAcc[i][0]
            input[0][i][1] = bodyAcc[i][1]
            input[0][i][2] = bodyAcc[i][2]
            input[0][i][3] = bodyGyro[i][0]
            input[0][i][4] = bodyGyro[i][1]
            input[0][i][5] = bodyGyro[i][2]
            input[0][i][6] = totalAcc[i][0]
            input[0][i][7] = totalAcc[i][1]
            input[0][i][8] = totalAcc[i][2]
        }

        val output = Array(1) { FloatArray(6) }
        interpreter.run(input, output)

        var bestIndex = 0
        var bestScore = output[0][0]
        for (i in 1 until output[0].size) {
            if (output[0][i] > bestScore) {
                bestScore = output[0][i]
                bestIndex = i
            }
        }

        onResult(labels[bestIndex], bestScore)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}
