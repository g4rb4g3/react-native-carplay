import { ImageSourcePropType } from 'react-native';
import { NavigationStep } from './NavigationStep';
import { DistanceUnits } from './TravelEstimates';

export type NavigationRoutingInfo =
  | {
      type: 'routingInfo';
      loading?: false;
      junctionImage?: ImageSourcePropType;
      nextStep?: NavigationStep;
      distance: number;
      distanceUnits: DistanceUnits;
      step: NavigationStep;
    }
  | { type: 'routingInfo'; loading: true };

export type NavigationMessageInfo = {
  type: 'messageInfo';
  title: string;
  text?: string;
  image?: ImageSourcePropType;
};

export type NavigationInfo = NavigationRoutingInfo | NavigationMessageInfo;
