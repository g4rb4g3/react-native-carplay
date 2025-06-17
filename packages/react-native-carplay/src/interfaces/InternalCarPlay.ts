import type { NativeModule } from 'react-native';
import type { Maneuver } from './Maneuver';
import type { TravelEstimates } from './TravelEstimates';
import type { PauseReason } from './PauseReason';
import type { TripConfig } from 'src/navigation/Trip';
import type { TimeRemainingColor } from './TimeRemainingColor';
import type { TextConfiguration } from './TextConfiguration';
import type { AndroidAutoAlertConfig, ImageSize } from 'src/CarPlay';
import type { Action } from './Action';
import { CarColor } from './CarColor';
import { HeaderAction } from './Action';

export interface InternalCarPlay extends NativeModule {
  checkForConnection(): void;
  setRootTemplate(templateId: string, animated: boolean): void;
  pushTemplate(templateId: string, animated: boolean): void;
  popToTemplate(templateId: string, animated: boolean): void;
  popToRootTemplate(animated: boolean): void;
  popTemplate(animated: boolean): void;
  presentTemplate(templateId: string, animated: boolean): void;
  dismissTemplate(animated: boolean): void;
  enableNowPlaying(enabled: boolean): void;
  updateManeuvers(maneuvers: Maneuver[]): void;
  updateTravelEstimatesNavigationSession(index: number, estimates: TravelEstimates): void;
  cancelNavigationSession(): Promise<void>;
  finishNavigationSession(): Promise<void>;
  pauseNavigationSession(reason: PauseReason, description?: string): Promise<void>;
  createTrip(id: string, config: TripConfig): void;
  updateInformationTemplateItems(id: string, config: unknown): void;
  updateInformationTemplateActions(id: string, config: unknown): void;
  createTemplate(id: string, config: unknown, callback?: unknown): void;
  updateTemplate(id: string, config: unknown): void;
  invalidate(id: string): void;
  startNavigationSession(templateId: string, tripId: string): Promise<void>;
  updateTravelEstimatesForTrip(
    id: string,
    tripId: string,
    travelEstimates: TravelEstimates,
    timeRemainingColor: TimeRemainingColor,
  ): void;
  updateMapTemplateConfig(id: string, config: unknown): void;
  updateMapTemplateMapButtons(id: string, config: unknown): void;
  hideTripPreviews(id: string): void;
  showTripPreviews(id: string, previews: string[], config: TextConfiguration): void;
  showTripPreview(
    id: string,
    previews: string[],
    selectedTripId: string,
    config: TextConfiguration,
  ): void;
  showRouteChoicesPreviewForTrip(id: string, tripId: string, config: TextConfiguration): void;
  presentNavigationAlert(id: string, config: unknown, animated: boolean): void;
  dismissNavigationAlert(id: string, animated: boolean): Promise<boolean>;
  showPanningInterface(id: string, animated: boolean): void;
  dismissPanningInterface(id: string, animated: boolean): void;
  getMaximumListSectionCount(id: string): Promise<number>;
  getMaximumListItemCount(id: string): Promise<number>;
  getMaximumListItemImageSize(id: string): Promise<ImageSize>;
  getMaximumNumberOfGridImages(id: string): Promise<number>;
  getMaximumListImageRowItemImageSize(id: string): Promise<ImageSize>;
  reactToSelectedResult(status: boolean): void;
  updateListTemplateSections(id: string, config: unknown): void;
  updateListTemplateItem(id: string, config: unknown): void;
  reactToUpdatedSearchText(id: string, items: unknown): void;
  updateTabBarTemplates(id: string, config: unknown): void;
  activateVoiceControlState(id: string, identifier: string): void;
  getRootTemplate(callback: (templateId: string) => void): void;
  getTopTemplate(callback: (templateId: string) => void): void;
  createDashboard(id: string, config: unknown): void;
  checkForDashboardConnection(): void;
  updateDashboardShortcutButtons(config: unknown): void;
  initCluster(clusterId: string, config: unknown): void;
  checkForClusterConnection(clusterId: string): void;
  /**
   * @namespace Android
   */
  reload(): void;
  /**
   * @namespace Android
   */
  toast(message: string, isLongDurationToast: boolean): void;
  /**
   * @namespace Android
   */
  alert(config: Omit<AndroidAutoAlertConfig, 'actions'> & { actions?: Action[] }): void;
  /**
   * @namespace Android
   */
  dismissAlert(id: number): void;
  /**
   * @namespace Android
   */
  navigationStarted: () => Promise<void>;
  /**
   * @namespace Android
   */
  navigationEnded: () => Promise<void>;
  /**
   * @namespace Android
   */
  startTelemetryObserver: () => Promise<string>;
  /**
   * @namespace Android
   */
  stopTelemetryObserver: () => void;
  /**
   * @namespace Android
   */
  notify: (title: string, text: string, largeIcon: unknown) => void;
  /**
   * Shows a message template to the user asking for specific permissions
   * @param permissions Permissions to request from the user
   * @param message Message to show on the template
   * @param primaryAction Primary action that can be pressed while the car is parked only
   * @returns Promise in case permissions were granted or denied, or null in case a back button was specified has headerAction and pressed by the user
   * @namespace Android
   */
  requestPermissions: (
    permissions: Array<String>,
    message: string,
    primaryAction: Action,
    headerAction: HeaderAction,
  ) => Promise<{ granted: Array<string>; denied: Array<string> } | null>;
}
