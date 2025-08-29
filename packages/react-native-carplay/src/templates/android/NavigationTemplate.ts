import { AndroidRenderTemplates } from '../../interfaces/AndroidRenderTemplates';
import { AndroidAction } from '../../interfaces/Action';
import { CarColor } from '../../interfaces/CarColor';
import { NavigationInfo } from '../../interfaces/NavigationInfo';
import {
  AndroidNavigationBaseTemplate,
  AndroidNavigationBaseTemplateConfig,
} from './AndroidNavigationBaseTemplate';
import { CarPlay } from '../../CarPlay';
import { AndroidTravelEstimates, NavigationTrip } from '../../interfaces/NavigationStep';
import { ColorValue } from 'react-native';

export interface NavigationTemplateConfig
  extends Omit<AndroidNavigationBaseTemplateConfig, 'component'> {
  /**
   * Your component to render inside Android Auto Map view
   * NavigationTemplate is required to have this
   */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  component: React.ComponentType<any>;

  /**
   * Sets an ActionStrip with a list of template-scoped actions for this template.
   * The Action buttons in Map Based Template are automatically adjusted based on the screen size. On narrow width screen, icon Actions show by default. If no icon specify, showing title Actions instead. On wider width screen, title Actions show by default. If no title specify, showing icon Actions instead.
   * Requirements This template allows up to 4 Actions in its ActionStrip. Of the 4 allowed Actions, it can either be a title Action as set via setTitle, or a icon Action as set via setIcon.
   */
  actions: Array<AndroidAction>;
  /**
   * Sets the background color to use for the navigation information.
   * Depending on contrast requirements, capabilities of the vehicle screens, or other factors, the color may be ignored by the host or overridden by the vehicle system.
   */
  backgroundColor?: CarColor | ColorValue;
  /**
   * Sets the TravelEstimate to the final destination.
   */
  travelEstimate?: AndroidTravelEstimates;
  /**
   *
   */
  trip?: NavigationTrip;
  /**
   * Sets an ActionStrip with a list of map-control related actions for this template, such as pan or zoom.
   * The host will draw the buttons in an area that is associated with map controls.
   * If the app does not include the PAN button in this ActionStrip, the app will not receive the user input for panning gestures from SurfaceCallback methods, and the host will exit any previously activated pan mode.
   * Requirements This template allows up to 4 Actions in its map ActionStrip. Only Actions with icons set via setIcon are allowed.
   */
  mapButtons?: Array<AndroidAction>;
  /**
   * Sets the navigation information to display on the template.
   * Unless set with this method, navigation info won't be displayed on the template.
   */
  navigationInfo?: NavigationInfo;

  /**
   * Notifies the app to stop active navigation, which may occurs when another source such as the car head unit starts navigating.
   * When receiving this callback, the app must stop all routing including navigation voice guidance, routing-related notifications, and updating trip information
   */
  onDidCancelNavigation?(): void;

  /**
   * Notifies the app that, from this point onwards, when the user chooses to navigate to a destination, the app should start simulating a drive towards that destination.
   * This mode should remain active until finishCarApp is called.
   * This functionality is used to allow verifying the app's navigation capabilities without being in an actual car.
   */
  onAutoDriveEnabled: () => void;
}

/**
 * A template for showing navigation information.
 * This template has two independent sections which can be updated:
 * - Navigation information such as routing instructions or navigation-related messages.
 * - Travel estimates to the destination.
 */
export class NavigationTemplate extends AndroidNavigationBaseTemplate<NavigationTemplateConfig> {
  public get type(): string {
    return AndroidRenderTemplates.Navigation;
  }

  public navigationStarted() {
    return CarPlay.bridge.navigationStarted();
  }

  public navigationEnded() {
    return CarPlay.bridge.navigationEnded();
  }
}
