import { TravelEstimates } from './TravelEstimates';
import { ColorValue, ImageSourcePropType, ProcessedColorValue } from 'react-native';

export interface AttributedInstructionVariant {
  text: string;
  /**
   * attachment image size can be up to 64x16 points.
   */
  image?: ImageSourcePropType;
  /**
   * position of the image in the text, if missing the image will be appended to the text
   */
  imagePosition?: number;
}

/**
 * Navigation instructions and distance from the previous maneuver.
 */
export interface Maneuver {
  junctionImage?: ImageSourcePropType;
  initialTravelEstimates?: TravelEstimates;
  symbolImage?: ImageSourcePropType;
  /**
   * The size of the image in points. Please read the CarPlay App Programming Guide
   * to get the recommended size.
   */
  symbolImageSize?: { width: number; height: number };
  /**
   * Allows the supplied symbol image to be tinted
   * via a color, ie. 'red'. This functionality would usually
   * be available via the `<Image>` tag but carplay requires
   * an image asset to this tinting is done on the native side.
   * The tinted image is supplied as darkImage so CarPlay decides on when to show the tinted version!
   * If a string is supplied, it will be passed to `processColor`.
   * You may also use `processColor` yourself.
   */
  tintSymbolImage?: null | number | ColorValue | ProcessedColorValue;
  instructionVariants: string[];
  attributedInstructionVariants?: AttributedInstructionVariant[];

  // not yet implemented
  dashboardInstructionVariants?: string[];
  notificationInstructionVariants?: string[];

  // these are required for the "standard instrument cluster" and/or "head up display"
  maneuverType?: ManeuverType;
  junctionType?: JunctionType;
  trafficSide?: TrafficSide;
  /**
   * specify angle between -180° and +180° shown as the active exit
   */
  junctionExitAngle?: number;
  /**
   * specify angles (or a single angle) between -180° and +180°
   */
  junctionElementAngles?: Array<number>;
  roadFollowingManeuverVariants?: Array<string>;
  highwayExitLabel?: string;
  lanes?: Array<{ angles: Array<number>; highlightedAngle?: number, status?: 'preferred' | 'good' | 'notgood' }>;
}

export enum ManeuverType {
  NoTurn = 0,
  LeftTurn = 1,
  RightTurn = 2,
  StraightAhead = 3,
  UTurn = 4,
  FollowRoad = 5,
  EnterRoundabout = 6,
  ExitRoundabout = 7,
  OffRamp = 8,
  OnRamp = 9,
  ArriveEndOfNavigation = 10,
  StartRoute = 11,
  ArriveAtDestination = 12,
  KeepLeft = 13,
  KeepRight = 14,
  Enter_Ferry = 15,
  ExitFerry = 16,
  ChangeFerry = 17,
  StartRouteWithUTurn = 18,
  UTurnAtRoundabout = 19,
  LeftTurnAtEnd = 20,
  RightTurnAtEnd = 21,
  HighwayOffRampLeft = 22,
  HighwayOffRampRight = 23,
  ArriveAtDestinationLeft = 24,
  ArriveAtDestinationRight = 25,
  UTurnWhenPossible = 26,
  ArriveEndOfDirections = 27,
  RoundaboutExit1 = 28,
  RoundaboutExit2 = 29,
  RoundaboutExit3 = 30,
  RoundaboutExit4 = 31,
  RoundaboutExit5 = 32,
  RoundaboutExit6 = 33,
  RoundaboutExit7 = 34,
  RoundaboutExit8 = 35,
  RoundaboutExit9 = 36,
  RoundaboutExit10 = 37,
  RoundaboutExit11 = 38,
  RoundaboutExit12 = 39,
  RoundaboutExit13 = 40,
  RoundaboutExit14 = 41,
  RoundaboutExit15 = 42,
  RoundaboutExit16 = 43,
  RoundaboutExit17 = 44,
  RoundaboutExit18 = 45,
  RoundaboutExit19 = 46,
  SharpLeftTurn = 47,
  SharpRightTurn = 48,
  SlightLeftTurn = 49,
  SlightRightTurn = 50,
  ChangeHighway = 51,
  ChangeHighwayLeft = 52,
  ChangeHighwayRight = 53,
}

export enum JunctionType {
  Intersection = 0, // single intersection with roads coming to a common point
  Roundabout = 1, // roundabout, junction elements represent roads exiting the roundabout
}

export enum TrafficSide {
  Right = 0, // counterclockwise for roundabouts
  Left = 1, // clockwise for roundabouts
}
