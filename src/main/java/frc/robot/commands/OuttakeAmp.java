package frc.robot.commands;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import edu.wpi.first.wpilibj2.command.WaitUntilCommand;
import frc.robot.constants.ArmConstants;
import frc.robot.constants.ShooterConstants;
import frc.robot.constants.StorageIndexConstants;
import frc.robot.constants.miscConstants.VisionConstants;
import frc.robot.subsystems.Drivetrain;
import frc.robot.subsystems.gpm.Arm;
import frc.robot.subsystems.gpm.Shooter;
import frc.robot.subsystems.gpm.StorageIndex;

/**
 * Scores in the amp
 */
public class OuttakeAmp extends SequentialCommandGroup {
  /**
   * Scores in the amp
   * 
   * @param arm     The arm
   * @param index   The indexer
   * @param shooter The shooter
   */
  public OuttakeAmp(Arm arm, StorageIndex index, Shooter shooter) {
    addCommands(
        // Get the shooter to the right speed while moving arm
        new InstantCommand(() -> shooter.setTargetRPM(ShooterConstants.AMP_OUTTAKE_RPM)),
        // Move arm
        new ArmToPos(arm, ArmConstants.ampSetpoint),
        // Score in amp
        new InstantCommand(() -> index.ejectAmpFront()),
        // Wait until note is scored
        new WaitCommand(StorageIndexConstants.ejectAmpFrontTimeout),
        // Set everything back to default state
        new InstantCommand(() -> shooter.setTargetRPM(0)),
        new InstantCommand(() -> index.stopIndex()),
        new InstantCommand(() -> arm.setAngle(ArmConstants.stowedSetpoint)));
  }

  /**
   * Aligns to the amp and scores
   * 
   * @param arm     The arm
   * @param index   The indexer
   * @param shooter The shooter
   * @param drive   The drivetrain
   */
  public OuttakeAmp(Arm arm, StorageIndex index, Shooter shooter, Drivetrain drive) {
    Pose2d ampPose = DriverStation.getAlliance().get() == Alliance.Red ? VisionConstants.RED_AMP_POSE
        : VisionConstants.BLUE_AMP_POSE;
    addCommands(
        new SequentialCommandGroup(
            // Wait until the robot is close enough to the amp to start scoring
            new WaitUntilCommand((() -> {
              Transform2d dist = drive.getPose().minus(ampPose);
              return dist.getTranslation().getNorm() < VisionConstants.AMP_TOLERANCE_DISTANCE
                  && Math.abs(dist.getRotation().getRadians()) < VisionConstants.AMP_TOLERANCE_ANGLE;
            })),
            // Run the other version of this command to score in the amp
            new OuttakeAmp(arm, index, shooter)).deadlineWith(
                // Go to the pose and stay at it until the command finishes
                new GoToPose(ampPose, drive)));
  }
}