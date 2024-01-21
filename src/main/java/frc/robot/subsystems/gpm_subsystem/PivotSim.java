package frc.robot.subsystems.gpm_subsystem;

import com.ctre.phoenix.motorcontrol.TalonFXSimCollection;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonFX;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.simulation.BatterySim;
import edu.wpi.first.wpilibj.simulation.SingleJointedArmSim;
import edu.wpi.first.wpilibj.smartdashboard.Mechanism2d;
import edu.wpi.first.wpilibj.smartdashboard.MechanismLigament2d;
import edu.wpi.first.wpilibj.smartdashboard.MechanismRoot2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj.util.Color8Bit;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.constants.PivotSimConstants;

public class PivotSim extends SubsystemBase{

    private final WPI_TalonFX m_wristMotor;
    private final PIDController m_controller;
    private double m_motorPower; 
  
    private TalonFXSimCollection m_motorSim; 
    private Mechanism2d m_wristDisplay; 
    private MechanismRoot2d m_pivot; 
    private MechanismLigament2d m_stationary; 
    private MechanismLigament2d m_moving; 
    private SingleJointedArmSim m_wristPhysicsSim; 
    
    public PivotSim() {

        m_wristMotor = new WPI_TalonFX(1);
        m_controller = new PIDController(PivotSimConstants.kP, PivotSimConstants.kI, PivotSimConstants.kD);
        if(RobotBase.isSimulation()){

            m_motorSim = m_wristMotor.getSimCollection();
      
            m_wristPhysicsSim = new SingleJointedArmSim(
              PivotSimConstants.GEAR_BOX, 
              PivotSimConstants.GEAR_RATIO, 
              PivotSimConstants.MOMENT_OF_INERTIA, 
              PivotSimConstants.ARM_LENGTH, 
              PivotSimConstants.MIN_POS, 
              PivotSimConstants.MAX_POS,
              false,
              PivotSimConstants.STARTING_POS
            );

            m_wristPhysicsSim.setState(VecBuilder.fill(PivotSimConstants.MIN_POS,0)); 

            m_wristMotor.setSelectedSensorPosition(0);
      
            m_wristDisplay = new Mechanism2d(90, 90);
      
            m_pivot = m_wristDisplay.getRoot("ArmPivot", 45, 45);
        
            m_stationary = m_pivot.append(new MechanismLigament2d("Stationary", 60, -180));
              
            m_moving = m_pivot.append(
                new MechanismLigament2d(
                    "Moving",
                    30,
                    Units.radiansToDegrees(m_wristPhysicsSim.getAngleRads()),
                    6,
                    new Color8Bit(Color.kYellow)));
      
            SmartDashboard.putData("Wrist with PID", m_wristDisplay); 
    }

    SmartDashboard.putData(m_controller); 

    setSetpoint(Units.radiansToDegrees(PivotSimConstants.MIN_POS));

    }

    @Override
    public void periodic() {
    
        moveMotorsWithPID();

        m_wristPhysicsSim.setInput(m_wristMotor.get() * BatterySim.calculateDefaultBatteryLoadedVoltage(m_wristPhysicsSim.getCurrentDrawAmps()));
        
        m_wristPhysicsSim.update(0.020);
        
        m_motorSim.setIntegratedSensorRawPosition(physicsSimRadsToTicks(m_wristPhysicsSim.getAngleRads()+PivotSimConstants.kSetpointOffsetRads)); 
    
        m_moving.setAngle(Units.radiansToDegrees(m_wristPhysicsSim.getAngleRads()));

  }

    public void setSetpoint(double setpoint){
        
        m_controller.reset();

        setpoint = Units.degreesToRadians(setpoint)+PivotSimConstants.kSetpointOffsetRads;

        setpoint = MathUtil.clamp(setpoint, PivotSimConstants.MIN_POS+PivotSimConstants.kSetpointOffsetRads,PivotSimConstants.MAX_POS+PivotSimConstants.kSetpointOffsetRads);
    
        m_controller.setSetpoint(setpoint); 

  }

    public void moveMotorsWithPID(){

        m_motorPower = m_controller.calculate(m_wristMotor.getSelectedSensorPosition()*PivotSimConstants.kEncoderTicksToRadsConversion);

        MathUtil.clamp(m_motorPower, PivotSimConstants.MIN_POW, PivotSimConstants.MAX_POW); 

        m_wristMotor.set(m_motorPower);
    
  } 

    public int physicsSimRadsToTicks(double rads){

        int rawPos = (int)((rads/(2*Math.PI))*2048);

        return rawPos;  

    }
  
  }
      