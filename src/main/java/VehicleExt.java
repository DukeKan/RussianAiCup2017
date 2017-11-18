import model.Vehicle;
import model.VehicleType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static model.VehicleType.*;
import static model.VehicleType.ARRV;
import static model.VehicleType.TANK;

/**
 * Created by DukeKan on 12.11.2017.
 */
public class VehicleExt {
    public static Set<VehicleType> getPreferredTargetType(VehicleType vehicleType) {
        switch (vehicleType) {
            case FIGHTER:
                return of(HELICOPTER).collect(toSet());
            case HELICOPTER:
                return of(TANK).collect(toSet());
            case IFV:
                return of(HELICOPTER).collect(toSet());
            case TANK:
                return of(IFV).collect(toSet());
            default:
                return of(TANK, IFV).collect(toSet());
        }
    }

    public static Set<VehicleType> getAnyTargetType(VehicleType vehicleType) {
        switch (vehicleType) {
            case FIGHTER:
                return of(HELICOPTER, FIGHTER).collect(toSet());
            case HELICOPTER:
                return of(TANK, IFV, HELICOPTER, FIGHTER, ARRV).collect(toSet());
            case IFV:
                return of(TANK, IFV, HELICOPTER, FIGHTER, ARRV).collect(toSet());
            case TANK:
                return of(TANK, IFV, HELICOPTER, FIGHTER, ARRV).collect(toSet());
            default:
                return of(TANK, IFV, HELICOPTER, FIGHTER).collect(toSet());
        }
    }

    public static List<VehicleType> getVehicleTypes() {
        List<VehicleType> vehicleTypes = new ArrayList<>(5);
        vehicleTypes.add(ARRV);
        vehicleTypes.add(TANK);
        vehicleTypes.add(IFV);
        vehicleTypes.add(HELICOPTER);
        vehicleTypes.add(FIGHTER);
        return vehicleTypes;
    }

    public static double getAttackRange(Vehicle vehicle) {
        if (isGround(vehicle.getType())) {
            return vehicle.getGroundAttackRange();
        } else {
            return vehicle.getAerialAttackRange();
        }
    }

    public static boolean isGround(VehicleType vehicleType) {
        List<VehicleType> groundTypes = Stream.of(TANK, ARRV, IFV).collect(toList());
        return groundTypes.contains(vehicleType);
    }
}
