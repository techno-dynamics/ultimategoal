package io.arct.techno.ftc

import io.arct.ftc.eventloop.LinearOperationMode
import io.arct.ftc.eventloop.OperationMode
import io.arct.rl.hardware.motors.Motor
import io.arct.rl.hardware.motors.Servo
import io.arct.rl.robot.Robot
import io.arct.rl.units.*
import io.arct.techno.ftc.cv.RingDetector
import io.arct.techno.ftc.cv.RingState
import io.arct.techno.ftc.util.*

@OperationMode.Register(OperationMode.Type.Autonomous, "Auto (L5)")
class AutoL5 : LinearOperationMode() {
    private val shooter = Motor.get("m5", 5800.revpm)
    private val wobble = Motor.get("m7", 223.revpm)

    private val s1 by Servo
    private val s2 by Servo
    private val s3 by Servo

    private val secrets: Secret = PersistentObject.load("/sdcard/secrets.dat")
    private val calibration: CalibrationData = PersistentObject.load("/sdcard/calibration.dat")

    private val robot: Robot = robot()
    private val detector = RingDetector(this, secrets.vuforia)

    val shooterPower = -1.0
    val shootDelay = 500L
    val shooterPositionA = 0.7
    val shooterPositionB = 1.0

    override suspend fun init() {
        s2.position = -0.8
        s3.position = calibration.shooterHigh
    }

    override suspend fun run() {
        val state = detector.scan()

        robot.move(Angle.Forward, 140.cm, 1.mps)
        robot.move(Angle.Left, 35.cm, 1.mps)

        shoot(3)

        when (state) {
            RingState.None -> {
                robot.move(Angle.Left, 20.cm, 1.mps)
                robot.move(Angle.Forward, 50.cm, 1.mps)
            }

            RingState.Partial -> {
                robot.move(Angle.Right, 20.cm, 1.mps)
                robot.move(Angle.Forward, 110.cm, 1.mps)
            }

            RingState.Full -> {
                robot.move(Angle.Left, 25.cm, 1.mps)
                robot.move(Angle.Forward, (80 + 50).cm, 1.mps)

                robot.move(Angle.Forward, 1.mps)
                Thread.sleep(1000L)
                robot.stop()
            }
        }

        wobble.power(-.8)
        Thread.sleep(1000L)
        wobble.stop()

        Thread.sleep(1500L)
        s2.position = 0.8
        Thread.sleep(1500L)

        wobble.power(.6)
        Thread.sleep(500L)
        wobble.stop()

        when (state) {
            RingState.None -> {
                robot.move(Angle.Right, 25.cm, 1.mps)
                robot.move(Angle.Backward, 25.cm, 1.mps)
            }

            RingState.Partial -> {
                robot.move(Angle.Backward, 85.cm, 1.mps)
                robot.move(Angle.Left, 20.cm, 1.mps)
            }

            RingState.Full -> {
                robot.move(Angle.Backward, (100 + 25).cm, 1.mps)
            }
        }
    }

    private fun shoot(n: Int) {
        shooter.power(shooterPower)
        Thread.sleep(1000L)

        for (i in 0..n) {
            Thread.sleep(shootDelay * 2)
            s1.position = shooterPositionA
            Thread.sleep(shootDelay)
            s1.position = shooterPositionB
        }

        shooter.stop()
    }
}