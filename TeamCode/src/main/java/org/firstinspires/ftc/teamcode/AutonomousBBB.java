package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvPipeline;
import org.openftc.easyopencv.OpenCvWebcam;

@Autonomous
public class AutonomousBBB extends LinearOpMode {
    HardwareBIGBRAINBOTS robot = new HardwareBIGBRAINBOTS();
    OpenCvWebcam webCam;
    DuckDetectionPipeline pipeline;
    public enum DuckPosition
    {
        LEFT,
        CENTER,
        RIGHT
    }

    @Override
    public void runOpMode() {
        //super.runOpMode();
        final double COUNTS_PER_MOTOR_REV  =28;
        final double DRIVE_GEAR_REDUCTION  =1;
        final double WHEEL_DIAMETER_INCHES = 4;
        final double COUNTS_PER_INCH = 44.64;
        final double STRAFE_COUNTS_PER_INCH = 49.02;

        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        webCam = OpenCvCameraFactory.getInstance().createWebcam(hardwareMap.get(WebcamName.class,"Webcam 1"),cameraMonitorViewId);
        pipeline = new DuckDetectionPipeline();
        webCam.setPipeline(pipeline);

        webCam.openCameraDeviceAsync(new OpenCvCamera.AsyncCameraOpenListener()
        {
            @Override
            public void onOpened()
            {
                webCam.startStreaming(320,240, OpenCvCameraRotation.UPRIGHT);
            }

            @Override
            public void onError(int errorCode)
            {
                /*
                 * This will be called if the camera could not be opened
                 */
            }
        });

        robot.init(this.hardwareMap);
        robot.extendArm(1, -100);
        robot.DumperServo.setPosition(0.5);
        robot.extendArm(1, 100);

        waitForStart();
        DuckPosition position = pipeline.getAnalysis();
        telemetry.addData("Analysis", pipeline.getAnalysis());
        telemetry.update();

        robot.strafe(1,-(int)(28*STRAFE_COUNTS_PER_INCH));
        robot.strafe(0.5,-(int)(1*STRAFE_COUNTS_PER_INCH));
        //sleep(30);
       //robot.gyroTurn(1, -45, 0.01);

        robot.CarouselMotor.setPower(-1);
        sleep(2000);
        robot.CarouselMotor.setPower(0);
        robot.CarouselMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        robot.strafe(1,(int)(50*STRAFE_COUNTS_PER_INCH));

        robot.gyroTurn(1, 180, 0.01);
        if(position==DuckPosition.LEFT) {
            robot.extendArm(1, -2000);
        }
        if(position==DuckPosition.CENTER) {
            //robot.extendArm(1, -3000);
        }
        if(position==DuckPosition.RIGHT) {
            //robot.extendArm(1, -4000);
        }

        robot.drive(1,(int)(-19*COUNTS_PER_INCH));



        robot.DumperServo.setPosition(1);
        sleep(500);
        robot.DumperServo.setPosition(0.5);
        /*
        switch(position)
        {
            case LEFT:
                robot.extendArm(-1, -1500);
                break;
            case CENTER:
                robot.extendArm(-1, -2000);
                break;
            case RIGHT:
                robot.extendArm(-1, -2500);
                break;
            default:

                break;
        }

         */
        robot.extendArm(1, 1000);
        robot.strafe(1,(int)(54*STRAFE_COUNTS_PER_INCH));
        robot.drive(1, (int)(-12*COUNTS_PER_INCH));

        telemetry.addData("strafe", "finished");
        telemetry.update();
    }

    public static class DuckDetectionPipeline extends OpenCvPipeline
    {

         // An enum to define the Duck position

        // Some color constants
        static final Scalar BLUE = new Scalar(0, 0, 255);
        static final Scalar GREEN = new Scalar(0, 255, 0);

        /*
         * The core values which define the location and size of the sample regions
         */
        static final Point REGION1_TOPLEFT_ANCHOR_POINT = new Point(30,98);
        static final Point REGION2_TOPLEFT_ANCHOR_POINT = new Point(160,98);
        static final Point REGION3_TOPLEFT_ANCHOR_POINT = new Point(280,98);
        static final int REGION_WIDTH = 30;
        static final int REGION_HEIGHT = 30;

        /*
         * Points which actually define the sample region rectangles, derived from above values
         *
         * Example of how points A and B work to define a rectangle
         *
         *   ------------------------------------
         *   | (0,0) Point A                    |
         *   |                                  |
         *   |                                  |
         *   |                                  |
         *   |                                  |
         *   |                                  |
         *   |                                  |
         *   |                  Point B (70,50) |
         *   ------------------------------------
         *
         */
        Point region1_pointA = new Point(
                REGION1_TOPLEFT_ANCHOR_POINT.x,
                REGION1_TOPLEFT_ANCHOR_POINT.y);
        Point region1_pointB = new Point(
                REGION1_TOPLEFT_ANCHOR_POINT.x + REGION_WIDTH,
                REGION1_TOPLEFT_ANCHOR_POINT.y + REGION_HEIGHT);
        Point region2_pointA = new Point(
                REGION2_TOPLEFT_ANCHOR_POINT.x,
                REGION2_TOPLEFT_ANCHOR_POINT.y);
        Point region2_pointB = new Point(
                REGION2_TOPLEFT_ANCHOR_POINT.x + REGION_WIDTH,
                REGION2_TOPLEFT_ANCHOR_POINT.y + REGION_HEIGHT);
        Point region3_pointA = new Point(
                REGION3_TOPLEFT_ANCHOR_POINT.x,
                REGION3_TOPLEFT_ANCHOR_POINT.y);
        Point region3_pointB = new Point(
                REGION3_TOPLEFT_ANCHOR_POINT.x + REGION_WIDTH,
                REGION3_TOPLEFT_ANCHOR_POINT.y + REGION_HEIGHT);

        /*
         * Working variables, LAB color space, Channel a, Channel b
         * Channel b is used to distinguish Yellow from Blue
         * Channel a is used to distinguish Yellow form Red, also works to distinguish from Blue
         */
        Mat region1_Cb, region2_Cb, region3_Cb;
        Mat region1_Ca, region2_Ca, region3_Ca;
        //   Mat BGR = new Mat();
        Mat LAB = new Mat();
        Mat Cb = new Mat();
        Mat Ca = new Mat();
        int avg1, avg2, avg3;

        // Volatile since accessed by OpMode thread w/o synchronization
        private volatile DuckPosition position = DuckPosition.LEFT;

        /*
         * This function takes the RGB frame, converts to LAB,
         * and extracts the Cb or Ca channel to the 'Cb', or 'Ca' variable
         */
        void inputToCb(Mat input)
        {
            Imgproc.cvtColor(input,LAB,Imgproc.COLOR_RGB2Lab);
            Core.extractChannel(LAB,Cb,2);
        }
        void inputToCa(Mat input){
            Imgproc.cvtColor(input,LAB,Imgproc.COLOR_RGB2Lab);
            Core.extractChannel(LAB,Ca,1);
        }

        @Override
        public void init(Mat firstFrame)
        {
            /*
             * We need to call this in order to make sure the 'Cb'
             * object is initialized, so that the submats we make
             * will still be linked to it on subsequent frames. (If
             * the object were to only be initialized in processFrame,
             * then the submats would become delinked because the backing
             * buffer would be re-allocated the first time a real frame
             * was crunched)
             */
            //inputToCb(firstFrame);   For Blue alliance
            inputToCa(firstFrame);
            /*
             * Submats are a persistent reference to a region of the parent
             * buffer. Any changes to the child affect the parent, and the
             * reverse also holds true.
             */
            //         region1_Cb = Cb.submat(new Rect(region1_pointA, region1_pointB));
            //         region2_Cb = Cb.submat(new Rect(region2_pointA, region2_pointB));
            //         region3_Cb = Cb.submat(new Rect(region3_pointA, region3_pointB));
            region1_Ca = Ca.submat(new Rect(region1_pointA, region1_pointB));
            region2_Ca = Ca.submat(new Rect(region2_pointA, region2_pointB));
            region3_Ca = Ca.submat(new Rect(region3_pointA, region3_pointB));
        }

        @Override
        public Mat processFrame(Mat input)
        {

            // Get the Cb or Ca channel of the input frame after conversion to LAB
            //    inputToCb(input);
            inputToCa(input);
            /*
             * Compute the average pixel value of each submat region. We're
             * taking the average of a single channel buffer, so the value
             * we need is at index 0. We could have also taken the average
             * pixel value of the 3-channel image, and referenced the value
             * at index 2 here.
             */
            //           avg1 = (int) Core.mean(region1_Cb).val[0];
            //           avg2 = (int) Core.mean(region2_Cb).val[0];
            //           avg3 = (int) Core.mean(region3_Cb).val[0];
            avg1 = (int) Core.mean(region1_Ca).val[0];
            avg2 = (int) Core.mean(region2_Ca).val[0];
            avg3 = (int) Core.mean(region3_Ca).val[0];
            /*
             * Draw a rectangle showing sample region 1 on the screen.
             * Simply a visual aid. Serves no functional purpose.
             */
            Imgproc.rectangle(
                    input, // Buffer to draw on
                    region1_pointA, // First point which defines the rectangle
                    region1_pointB, // Second point which defines the rectangle
                    BLUE, // The color the rectangle is drawn in
                    2); // Thickness of the rectangle lines

            /*
             * Draw a rectangle showing sample region 2 on the screen.
             * Simply a visual aid. Serves no functional purpose.
             */
            Imgproc.rectangle(
                    input, // Buffer to draw on
                    region2_pointA, // First point which defines the rectangle
                    region2_pointB, // Second point which defines the rectangle
                    BLUE, // The color the rectangle is drawn in
                    2); // Thickness of the rectangle lines

            /*
             * Draw a rectangle showing sample region 3 on the screen.
             * Simply a visual aid. Serves no functional purpose.
             */
            Imgproc.rectangle(
                    input, // Buffer to draw on
                    region3_pointA, // First point which defines the rectangle
                    region3_pointB, // Second point which defines the rectangle
                    BLUE, // The color the rectangle is drawn in
                    2); // Thickness of the rectangle lines


            /*
             * Find the max of the 3 averages
             */
            //       int maxOneTwo = Math.max(avg1, avg2);
            //       int max = Math.max(maxOneTwo, avg3);
            int minOneTwo = Math.min(avg1,avg2);
            int min = Math.min(minOneTwo,avg3);

            /*
             * Now that we found the max, we actually need to go and
             * figure out which sample region that value was from
             */
            if(min == avg1) //if(max == avg1) // Was it from region 1?
            {
                position = DuckPosition.LEFT; // Record our analysis

                /*
                 * Draw a solid rectangle on top of the chosen region.
                 * Simply a visual aid. Serves no functional purpose.
                 */
                Imgproc.rectangle(
                        input, // Buffer to draw on
                        region1_pointA, // First point which defines the rectangle
                        region1_pointB, // Second point which defines the rectangle
                        GREEN, // The color the rectangle is drawn in
                        -1); // Negative thickness means solid fill
            }
            else if (min == avg2)//(max == avg2) // Was it from region 2?
            {
                position = DuckPosition.CENTER; // Record our analysis

                /*
                 * Draw a solid rectangle on top of the chosen region.
                 * Simply a visual aid. Serves no functional purpose.
                 */
                Imgproc.rectangle(
                        input, // Buffer to draw on
                        region2_pointA, // First point which defines the rectangle
                        region2_pointB, // Second point which defines the rectangle
                        GREEN, // The color the rectangle is drawn in
                        -1); // Negative thickness means solid fill
            }
            else if(min == avg3)//(max == avg3) // Was it from region 3?
            {
                position = DuckPosition.RIGHT; // Record our analysis
                /*
                 * Draw a solid rectangle on top of the chosen region.
                 * Simply a visual aid. Serves no functional purpose.
                 */
                Imgproc.rectangle(
                        input, // Buffer to draw on
                        region3_pointA, // First point which defines the rectangle
                        region3_pointB, // Second point which defines the rectangle
                        GREEN, // The color the rectangle is drawn in
                        -1); // Negative thickness means solid fill
            }

            /*
             * Render the 'input' buffer to the viewport. But note this is not
             * simply rendering the raw camera feed, because we called functions
             * to add some annotations to this buffer earlier up.
             */
            return input;
        }

        /*
         * Call this from the OpMode thread to obtain the latest analysis
         */
        public DuckPosition getAnalysis()
        {
            return position;
        }
    }
}



