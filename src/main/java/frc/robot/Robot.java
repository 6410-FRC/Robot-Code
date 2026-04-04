// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import pabeles.concurrency.ConcurrencyOps.Reset;

import com.revrobotics.PersistMode;
import com.revrobotics.ResetMode;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.SparkLowLevel.MotorType; 
import com.revrobotics.spark.config.SparkMaxConfig; 

import edu.wpi.first.wpilibj.drive.DifferentialDrive;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.XboxController;
/**
 * The methods in this class are called automatically corresponding to each mode, as described in
 * the TimedRobot documentation. If you change the name of this class or the package after creating
 * this project, you must also update the Main.java file in the project.
 */
public class Robot extends TimedRobot {
  private static final String kDefaultAuto = "Default: Backup from Hub then Launch";
  private static final String kLauncherFromCenter = "Launch from Center";
  private static final String kLaunchAndHumanLoad = "Launch from side, drive to loading zone for human to load";
  private static final String kLaunch = "Just LAUNCH!";
  private String m_autoSelected;
  private final SendableChooser<String> m_chooser = new SendableChooser<>();

  private final SparkMax LeftDrive1 = new SparkMax(1, MotorType.kBrushed);
  private final SparkMax LeftDrive2 = new SparkMax(2, MotorType.kBrushed);
  private final SparkMax RightDrive1 = new SparkMax(3, MotorType.kBrushed);
  private final SparkMax RightDrive2 = new SparkMax(4, MotorType.kBrushed);


  private final SparkMax IntakeAndLauncherRoller = new SparkMax(5, MotorType.kBrushless);
  private final SparkMax FeederRoller = new SparkMax(6, MotorType.kBrushless);

  private final DifferentialDrive myDrive = new DifferentialDrive(LeftDrive1, RightDrive1);

  private final Timer autoTimer = new Timer(); 
  private final Timer spinUpTimer = new Timer(); 

  private final XboxController driverController = new XboxController(0);

  //fuel mech parameters

  private static final double INTAKING_INTAKE_VOLTAGE = 9;
  private static final double INTAKING_FEEDER_VOLTAGE= -12; 

  private static final double LAUNCHING_LAUNCHER_VOLTAGE = 10; 
  private static final double LAUNCHING_FEEDER_VOLTAGE = 9; // Both are positive. Check direction using the robot.

  private static final double SPIN_UP_FEEDER_VOLTAGE= -6; 
  private static final double SPIN_UP_SECONDS= 1;

  //drive speed paramenters 

  private double driveSpeed = 1; 
  private double rotateSpeed = 1;
  private double elapsed_time = 0; 
    /**
     * This function is run when the robot is first started up and should be used for any
     * initialization code.
     */
    public Robot() {
      m_chooser.setDefaultOption("Default: Backup from Hub then Launch", kDefaultAuto);
      m_chooser.addOption("Launch from Center and stay", kLauncherFromCenter);
      m_chooser.addOption( "Launch from side, drive to human for load.", kLaunchAndHumanLoad);
      m_chooser.addOption( "Launch!", kLaunch);
      SmartDashboard.putData("Auto choices", m_chooser); 
      SmartDashboard.updateValues();

      //Drive Configs
      SparkMaxConfig driveConfig = new SparkMaxConfig();
      driveConfig.voltageCompensation(12);
      driveConfig.smartCurrentLimit(60);

      driveConfig.follow(LeftDrive1); 
      LeftDrive2.configure(driveConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

      driveConfig.follow(RightDrive1); 
      RightDrive2.configure(driveConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

      driveConfig.disableFollowerMode(); 
      driveConfig.inverted(false);
      RightDrive1.configure(driveConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

      driveConfig.inverted(true);
      LeftDrive1.configure(driveConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

      //Intake & Launcher Configs 

      SparkMaxConfig launcherConfig = new SparkMaxConfig();
      launcherConfig.smartCurrentLimit(60);
      launcherConfig.inverted(true);
      IntakeAndLauncherRoller.configure(launcherConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
      
      SparkMaxConfig feederConfig = new SparkMaxConfig();
      feederConfig.smartCurrentLimit(60);
      FeederRoller.configure(feederConfig,ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
    }
  
  
    /**
     * This function is called every 20 ms, no matter the mode. Use this for items like diagnostics
     * that you want ran during disabled, autonomous, teleoperated and test.
     *
     * <p>This runs after the mode specific periodic functions, but before LiveWindow and
     * SmartDashboard integrated updating.
     */
    @Override
    public void robotPeriodic() {}

    /**
     * This autonomous (along with the chooser code above) shows how to select between different
     * autonomous modes using the dashboard. The sendable chooser code works with the Java
     * SmartDashboard. If you prefer the LabVIEW Dashboard, remove all of the chooser code and
     * uncomment the getString line to get the auto name from the text box below the Gyro
     *
     * <p>You can add additional auto modes by adding additional comparisons to the switch structure
     * below with additional strings. If using the SendableChooser make sure to add them to the
     * chooser code above as well.
     */
    @Override
    public void autonomousInit() {
      m_autoSelected = m_chooser.getSelected();
      // m_autoSelected = SmartDashboard.getString("Auto Selector", kDefaultAuto);
      System.out.println("Auto selected: " + m_autoSelected);
      
      autoTimer.start();
      autoTimer.reset();
      elapsed_time = 0;
      
    }



    /** This function is called periodically during autonomous. */

  @Override
    public void autonomousPeriodic() {
      switch (m_autoSelected) {
        

        case kLaunchAndHumanLoad:  //

        if (autoTimer.get() < SPIN_UP_SECONDS)//Launches balls that have been preloaded up to 8
          { 
            elapsed_time = 1;
            IntakeAndLauncherRoller.setVoltage(LAUNCHING_LAUNCHER_VOLTAGE);
            FeederRoller.setVoltage(-SPIN_UP_FEEDER_VOLTAGE);
          }

          else if (autoTimer.get() < elapsed_time + 0.5) //
          { 
            elapsed_time += 0.5;
            IntakeAndLauncherRoller.setVoltage(LAUNCHING_LAUNCHER_VOLTAGE);
            FeederRoller.setVoltage(LAUNCHING_FEEDER_VOLTAGE);
          }

          else if(autoTimer.get() < elapsed_time + 0.5) //turn to face intake for human preload
          {
            elapsed_time += 0.5;
            IntakeAndLauncherRoller.setVoltage(0); 
            FeederRoller.setVoltage(0);
            myDrive.tankDrive(-.5,.5);//Should turn left
          }
          else if(autoTimer.get() < elapsed_time + 2.7)//drive to floor, load balls
          {
            elapsed_time += 2.7;
            myDrive.tankDrive(.45, .45);//Drive forward for 2.7 seconds
          }
          else if(autoTimer.get() < elapsed_time + 2) //drive to floor, load balls
          {
            elapsed_time += 2;
            IntakeAndLauncherRoller.setVoltage(-INTAKING_INTAKE_VOLTAGE); //load balls
            FeederRoller.setVoltage(-INTAKING_FEEDER_VOLTAGE);
            myDrive.tankDrive(0.85, 0.85);//Speed to execute needed
          }
          else //turn everything off
          {
            IntakeAndLauncherRoller.setVoltage(0);//resets it
            FeederRoller.setVoltage(0);
            myDrive.tankDrive(0, 0);
          } 

        break;
         //To seclect click "Launch from Center and Stay"
        case kLauncherFromCenter: //Use when at center to drive away from hub and launch balls
        //Make it go further
          if (autoTimer.get() < 0.6){
            myDrive.tankDrive(0.6, 0.6);
          }
          else if (autoTimer.get() < 1.6){
            IntakeAndLauncherRoller.setVoltage(-10);
          }
          else if (autoTimer.get() < 5){
            IntakeAndLauncherRoller.setVoltage(-10);
            FeederRoller.setVoltage(-9);
          }
          else if (autoTimer.get() < 5.4){
            myDrive.tankDrive(-0.6, -0.6);
          }
          else if (autoTimer.get() < 20){
            IntakeAndLauncherRoller.setVoltage(0);
            FeederRoller.setVoltage(0);
          }


          else //turn eveyrthing off
          {
            IntakeAndLauncherRoller.setVoltage(0);
            FeederRoller.setVoltage(0);
          } 


          break;
        case kLaunch:
          if(autoTimer.get() < 0.7){ 
             IntakeAndLauncherRoller.setVoltage(-10);
          }
          else if(autoTimer.get() < 11){
            IntakeAndLauncherRoller.setVoltage(-10);
            FeederRoller.setVoltage(-9);
          }
          else if(autoTimer.get() < 20){
            IntakeAndLauncherRoller.setVoltage(0);
            FeederRoller.setVoltage(0);
          }
          
            break;
            //To activate click "Default: Backup from Hub then Launch"
        case kDefaultAuto: //This code is to stand from the side of the Hub and launch
        default:
        //Speeds up Launcher for better launches
          if(autoTimer.get() < 0.7){ 
            IntakeAndLauncherRoller.setVoltage(-LAUNCHING_LAUNCHER_VOLTAGE);
          }
          else if(autoTimer.get() < 5){
            IntakeAndLauncherRoller.setVoltage(-LAUNCHING_LAUNCHER_VOLTAGE);
            FeederRoller.setVoltage(-9);
          }
          //Rotates robot to face nutrual zone
          else if (autoTimer.get() < 5.5) { 
            myDrive.tankDrive(-0.4, 0.4);  
          }
          //Drives to nutrual zone to collect balls
          else if (autoTimer.get() < 10) {
             myDrive.tankDrive(0.6, 0.6);
            IntakeAndLauncherRoller.setVoltage(-LAUNCHING_LAUNCHER_VOLTAGE);
            FeederRoller.setVoltage(-SPIN_UP_FEEDER_VOLTAGE);
          }
          //Should make the robot drive back to original spot
            else if (autoTimer.get() < 14.5) {
              myDrive.tankDrive(-0.6, -0.6);
            }
            //Angles the robot to how it was placed
            else if (autoTimer.get() < 14.5) {
              myDrive.tankDrive( 0.4, -0.4);
            }
            else if (autoTimer.get() < 15.2){
              IntakeAndLauncherRoller.setVoltage(-LAUNCHING_LAUNCHER_VOLTAGE);
            }
            //Fires balls collected
            else if (autoTimer.get() < 20) {
              IntakeAndLauncherRoller.setVoltage(-LAUNCHING_LAUNCHER_VOLTAGE);
              FeederRoller.setVoltage(-SPIN_UP_FEEDER_VOLTAGE);
            }

          else //turns everything off
          {
            IntakeAndLauncherRoller.setVoltage(0);
            FeederRoller.setVoltage(0);
            myDrive.tankDrive(0, 0); 
          }
          
          break;

      }
    
    }
  
    /** This function is called once when teleop is enabled. */
    @Override
    public void teleopInit() {
    autoTimer.stop();
    spinUpTimer.start();
  }

  //drive
@Override
public void teleopPeriodic() {

  // Drive
  myDrive.arcadeDrive(
      -driverController.getLeftY() * 0.9,
      -driverController.getRightX() * 0.9);

  // Launching mode (Right Bumper)
  if (driverController.getRightBumperButton()) {

    if (driverController.getRightBumperButtonPressed()) {
      spinUpTimer.reset(); // Timer is set to 0 and is ticking up.
    }

    // Timer is still ticking until SPIN_UP_SECONDs
    if (spinUpTimer.get() < 0.7) { 
      // This potentially feeds the ball into the laucher rollers
      IntakeAndLauncherRoller.setVoltage(-LAUNCHING_LAUNCHER_VOLTAGE); // this one controlls launchrer and feed clockwise
    } else {
      // spinUpTimer passed SPIN_UP_SECONDS
      // This launches the ball
      IntakeAndLauncherRoller.setVoltage(-LAUNCHING_LAUNCHER_VOLTAGE); //changed
      FeederRoller.setVoltage(-LAUNCHING_FEEDER_VOLTAGE); //Do not touch
    }
  }
  // Intake mode (Left Bumper)
  else if (driverController.getLeftBumperButton()) {
    IntakeAndLauncherRoller.setVoltage(-INTAKING_INTAKE_VOLTAGE); //changed
    FeederRoller.setVoltage(-INTAKING_FEEDER_VOLTAGE); //dont touch
  }

  // Eject mode (A Button)
  else if (driverController.getAButton()) {
    IntakeAndLauncherRoller.setVoltage(INTAKING_INTAKE_VOLTAGE);
    FeederRoller.setVoltage(INTAKING_FEEDER_VOLTAGE);
  }
  else if (driverController.getXButton()) {
    FeederRoller.setVoltage(INTAKING_FEEDER_VOLTAGE);
  }

  // Stop everything
  else {
    IntakeAndLauncherRoller.setVoltage(0);
    FeederRoller.setVoltage(0);
  }
}    @Override
    public void disabledInit() {}

    /** This function is called periodically when disabled. */
    @Override
    public void disabledPeriodic() {}

    /** This function is called once when test mode is enabled. */
    @Override
    public void testInit() {}

    /** This function is called periodically during test mode. */
    @Override
    public void testPeriodic() {}

    /** This function is called once when the robot is first started up. */
    @Override
    public void simulationInit() {}

    /** This function is called periodically whilst in simulation. */
    @Override
    public void simulationPeriodic() {}
  }