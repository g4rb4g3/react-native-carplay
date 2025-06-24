import * as React from 'react';
import { AppRegistry, Platform } from 'react-native';
import { NavigationAlertHideEvent, NavigationAlertShowEvent, Template, TemplateConfig } from '../Template';
import { CarPlay } from '../../CarPlay';
import {
  PanGestureWithTranslationEvent,
  PinchGestureEvent,
  PressEvent,
} from 'src/interfaces/GestureEvent';
import { Action, AndroidAction, getCallbackActionId } from '../../interfaces/Action';
import { Pane } from 'src/interfaces/Pane';
import { AndroidGridButton, GridButton } from 'src/interfaces/GridButton';

export interface AndroidNavigationBaseTemplateConfig extends TemplateConfig {
  /**
   * Your component to render inside Android Auto Map view
   * NavigationTemplate is required to have this, other templates might skip this if a NavigationTemplate is in place already
   */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  component?: React.ComponentType<any>;

  onDidShowPanningInterface?(): void;
  onDidDismissPanningInterface?(): void;

  /**
   * Fired when a pan gesture is happening
   * @param e coordinates for the pan event
   */
  onDidUpdatePanGestureWithTranslation?(e: PanGestureWithTranslationEvent): void;

  /**
   * Fired when a pinch gesture or a double tap happens
   * @param e PinchGestureEvent
   */
  onDidUpdatePinchGesture?(e: PinchGestureEvent): void;

  /**
   * Fired when a press event happens (single tap)
   * @param e PressEvent
   */
  onDidPress?(e: PressEvent): void;

  /**
   * Fired when the back button is pressed
   */
  onBackButtonPressed?(): void;

  /**
   * Fired when an alert dialog closes
   */
  onDidDismissAlert?(e: NavigationAlertHideEvent): void;

  /**
   * Fired when an alert dialog is about to be shown
   * @param id 
   */
  onWillShowAlert?(e: NavigationAlertShowEvent): void;
}

export class AndroidNavigationBaseTemplate<
  T extends AndroidNavigationBaseTemplateConfig,
> extends Template<T> {
  get eventMap() {
    return {
      didShowPanningInterface: 'onDidShowPanningInterface',
      didDismissPanningInterface: 'onDidDismissPanningInterface',
      didUpdatePanGestureWithTranslation: 'onDidUpdatePanGestureWithTranslation',
      didUpdatePinchGesture: 'onDidUpdatePinchGesture',
      didPress: 'onDidPress',
      didCancelNavigation: 'onDidCancelNavigation',
      didEnableAutoDrive: 'onAutoDriveEnabled',
      didSelectListItem: 'onItemSelect',
      backButtonPressed: 'onBackButtonPressed',
      didDismissNavigationAlert: 'onDidDismissAlert',
      willShowNavigationAlert: 'onWillShowAlert',
    };
  }

  private pressableCallbacks: {
    [key: string]: () => void;
  } = {};

  constructor(public config: T) {
    const { component, ...rest } = config;

    super(config);

    this.config = this.parseConfig({ type: this.type, ...rest, render: component != null });

    if (component) {
      // eslint-disable-next-line @typescript-eslint/no-this-alias
      const template = this;

      AppRegistry.registerComponent(
        this.id,
        () => props => React.createElement(component, { ...props, template: template }),
      );
    }

    const subscription = CarPlay.emitter.addListener(
      'buttonPressed',
      ({ buttonId }: { templateId?: string; buttonId: string }) => {
        const callback = this.pressableCallbacks[buttonId];
        if (callback == null || typeof callback !== 'function') {
          return;
        }
        callback();
      },
    );

    this.listenerSubscriptions.push(subscription);

    const callbackFn = Platform.select({
      android: ({ error }: { error?: string } = {}) => {
        error && console.error(error);
      },
    });

    CarPlay.bridge.createTemplate(this.id, this.config, callbackFn);
  }

  public parseConfig(
    config: TemplateConfig & {
      actions?: Array<AndroidAction>;
      mapButtons?: Array<AndroidAction>;
      navigateAction?: AndroidAction;
      pane?: Omit<Pane, 'actions'> & { actions?: Array<AndroidAction> };
      buttons?: Array<AndroidGridButton & { id?: string }>;
    },
  ) {
    const callbackIds: Array<string> = [];

    const { actions, mapButtons, navigateAction, pane, buttons, ...rest } = config;

    const updatedPane: (Omit<Pane, 'actions'> & { actions?: Array<Action> }) | undefined = pane
      ? {
          ...pane,
          actions: pane?.actions?.map(action => {
            const id = 'id' in action ? action.id : getCallbackActionId();
            if (id == null) {
              return action;
            }

            callbackIds.push(id);

            if (!('onPress' in action)) {
              return action;
            }

            const { onPress, ...actionRest } = action;
            this.pressableCallbacks[id] = onPress;
            return { ...actionRest, id };
          }),
        }
      : undefined;

    const updatedButtons: Array<GridButton> | undefined = buttons
      ? buttons.map(button => {
          const id = button.id ?? getCallbackActionId();
          callbackIds.push(id);

          if (!('onPress' in button)) {
            return button;
          }

          const { onPress, ...buttonRest } = button;
          this.pressableCallbacks[id] = onPress;
          return { ...buttonRest, id };
        })
      : undefined;

    const updatedConfig: TemplateConfig & {
      actions?: Array<Action>;
      mapButtons?: Array<Action>;
      navigateAction?: Action;
      pane?: Omit<Pane, 'actions'> & { actions?: Array<Action> };
      buttons?: Array<GridButton>;
    } = { ...rest, pane: updatedPane, buttons: updatedButtons };

    if (actions != null) {
      updatedConfig.actions = actions.map(action => {
        const id = 'id' in action ? action.id : getCallbackActionId();
        if (id == null) {
          return action;
        }

        callbackIds.push(id);

        if (!('onPress' in action)) {
          return action;
        }
        const { onPress, ...actionRest } = action;
        this.pressableCallbacks[id] = onPress;
        return { ...actionRest, id };
      });
    }

    if (mapButtons) {
      updatedConfig.mapButtons = mapButtons.map(mapButton => {
        const id = 'id' in mapButton ? mapButton.id : getCallbackActionId();
        if (id == null) {
          return mapButton;
        }

        callbackIds.push(id);

        if (!('onPress' in mapButton)) {
          return mapButton;
        }
        const { onPress, ...actionRest } = mapButton;
        this.pressableCallbacks[id] = onPress;
        return { ...actionRest, id };
      });
    }

    if (navigateAction) {
      const id = 'id' in navigateAction ? navigateAction.id : getCallbackActionId();

      if (id != null) {
        callbackIds.push(id);

        if ('onPress' in navigateAction) {
          const { onPress, ...actionRest } = navigateAction;
          this.pressableCallbacks[id] = onPress;
          updatedConfig.navigateAction = { ...actionRest, id };
        } else {
          updatedConfig.navigateAction = navigateAction;
        }
      }
    }

    this.pressableCallbacks = Object.fromEntries(
      Object.entries(this.pressableCallbacks).filter(([id]) => callbackIds.includes(id)),
    );

    return super.parseConfig(updatedConfig);
  }
}
