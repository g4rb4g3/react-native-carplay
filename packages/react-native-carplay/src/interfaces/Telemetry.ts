import { Permission } from 'react-native/types';

export type AndroidAutoPermissions =
  | Permission
  | 'com.google.android.gms.permission.CAR_FUEL'
  | 'com.google.android.gms.permission.CAR_SPEED'
  | 'com.google.android.gms.permission.CAR_MILEAGE'
  | 'android.car.permission.CAR_ENERGY'
  | 'android.car.permission.CAR_INFO'
  | 'android.car.permission.CAR_EXTERIOR_ENVIRONMENT'
  | 'android.car.permission.CAR_ENERGY_PORTS'
  | 'android.car.permission.CAR_SPEED';

type BaseTelemetryItem = {
  /**
   * timestamp in seconds when the value was received on native side
   */
  timestamp: number;
};
type NumericTelemetryItem = BaseTelemetryItem & {
  value: number;
};

type StringTelemetryItem = BaseTelemetryItem & {
  value: number;
};

export type Telemetry = {
  speed?: NumericTelemetryItem;
  fuelLevel?: NumericTelemetryItem;
  batteryLevel?: NumericTelemetryItem;
  range?: NumericTelemetryItem;
  odometer?: NumericTelemetryItem;
  vehicle?: {
    name?: StringTelemetryItem;
    year?: NumericTelemetryItem;
    manufacturer?: StringTelemetryItem;
  };
};

export type OnTelemetryCallback = (telemetry: Telemetry) => void;
