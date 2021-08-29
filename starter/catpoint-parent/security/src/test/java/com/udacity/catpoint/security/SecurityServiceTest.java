package com.udacity.catpoint.security;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.applilcation.StatusListener;
import com.udacity.catpoint.security.data.*;
import com.udacity.catpoint.security.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    private SecurityService securityService;
    private Sensor sensor;
    private final String randomString = UUID.randomUUID().toString();

    /* mock */
    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private StatusListener statusListener;

    private Sensor getNewSensor() {
        return new Sensor(randomString, SensorType.DOOR);
    }

    private Set<Sensor> getAllSensors(int count, boolean status) {
        Set<Sensor> sensors = new HashSet<>();
        for (int i = 0; i < count; i += 1) {
            sensors.add(new Sensor(randomString, SensorType.DOOR));
        }
        sensors.forEach(sensor -> sensor.setActive(status));
        return sensors;
    }

    @BeforeEach
    public void setUp() {
        securityService = new SecurityService(securityRepository, imageService);
        sensor = getNewSensor();
    }



    /* test 1 */
    @Test
    public void isArmedAndSensorActivated_setSystemToPendingAlarmStatus() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    /* test 2*/
    @Test
    public void isArmedAndSensorActivatedAndIsPending_setAlarmToAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    /* test 3 attention */
    @Test
    public void isPendingAlarmAndAllSensorsInactive_returnNoAlarmState() {
        Set<Sensor> testSensors = getAllSensors(5, false);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        testSensors.forEach(securityService::changeSensorActivationStatus);
        verify(securityRepository, times(5)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /* test 4 */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void isAlarmActive_shouldChangeSensorStateNotAffectAlarmState(boolean status) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, status);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    /* test 5 */
    @Test
    public void isSensorActive_shouldChangeSensorStateNotAffectAlarmState() {
        Sensor s1 = getNewSensor();
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        s1.setActive(true);
        securityService.changeSensorActivationStatus(s1, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    /* test 6 */
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
    public void isSensorDeactivatedWhileAreadyInactive_shouldNotChangeAlarmState(AlarmStatus alarmStatus) {
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any());
    }

    /* test 7 */
    @Test
    public void catImageIdentified_changeSystemToAlarmStatus() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    /* test 8 */
    @Test
    public void noCatImageIdentified_changeSystemToNoAlarm_shouldSensorsBeNotActive() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));
        Sensor s2 = getNewSensor();
        s2.setActive(false);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /* test 9 */
    @Test
    public void systemIsDisarmed_setStatusToNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    /* test 10 */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    public void systemIsArmed_resetAllSensorsToInactive(ArmingStatus armingStatus) {
        Set<Sensor> sensors = getAllSensors(5, true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(armingStatus);
        securityService.getSensors().forEach(s -> {
            assertFalse(s.getActive());
        });

    }

    /* test 11 */
    @Test
    public void systemIsArmedHome_idendifiedCat_setAlarmStatusToAlarm() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }


    /**
     * Application Requirements to Test:
     * If alarm is armed and a sensor becomes activated, put the system into pending alarm status. -> 1done
     * If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm. -> 2done
     * If pending alarm and all sensors are inactive, return to no alarm state. -> 3done
     * If alarm is active, change in sensor state should not affect the alarm state. -> 4done
     * If a sensor is activated while already active and the system is in pending state, change it to alarm state. -> 5done
     * If a sensor is deactivated while already inactive, make no changes to the alarm state. -> 6done
     * If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status. -> 7done
     * If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active. -> 8done
     * If the system is disarmed, set the status to no alarm. -> 9done
     * If the system is armed, reset all sensors to inactive. -> 10done
     * If the system is armed-home while the camera shows a cat, set the alarm status to alarm. -> 11done
     */
}
