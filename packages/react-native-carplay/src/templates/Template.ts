import { EmitterSubscription, Image, ImageSourcePropType, Platform } from 'react-native';
import { CarPlay } from '../CarPlay';
import { BarButton } from '../interfaces/BarButton';
import { AndroidRenderTemplates } from '../interfaces/AndroidRenderTemplates';

export interface BaseEvent {
  /**
   * Template id that fired the event
   */
  templateId: string;
}

export interface SearchButtonPressedEvent extends BaseEvent {
  searchText: string;
}

export interface BarButtonEvent extends BaseEvent {
  id: string;
}

export interface NavigationAlertShowEvent extends BaseEvent {
  navigationAlertId: number;
}

export interface NavigationAlertHideEvent extends BaseEvent {
  navigationAlertId: number;
  reason: 'none' | 'timeout' | 'system' | 'user' | 'notSupported' | 'unknown';
  /**
   * @namespace Android
   */
  type?: 'dismiss' | 'cancel';
}

export interface TemplateConfig {
  /**
   * Give the template your own ID. Must be unique.
   */
  id?: string;
  /**
   * An array of bar buttons to display on the leading side of the navigation bar.
   *
   * The navigation bar displays up to two buttons in the leading space. When including more than two buttons in the array, the system displays only the first two buttons.
   * @namespace iOS
   */
  leadingNavigationBarButtons?: BarButton[];
  /**
   * An array of bar buttons to display on the trailing side of the navigation bar.
   *
   * The navigation bar displays up to two buttons in the trailing space. When including more than two buttons in the array, the system displays only the first two buttons.
   * @namespace iOS
   */
  trailingNavigationBarButtons?: BarButton[];
  /**
   * UITabBarSystemItem
   */
  tabSystemItem?: number;
  /**
   * Name of system image for tab
   */
  tabSystemImageName?: string;
  /**
   * Image source for tab
   */
  tabImage?: ImageSourcePropType;
  /**
   * Set tab title
   */
  tabTitle?: string;
  /**
   * Fired before template appears
   * @param e Event
   */
  onWillAppear?(e: BaseEvent): void;
  /**
   * Fired before template disappears
   * @param e Event
   */
  onWillDisappear?(e: BaseEvent): void;
  /**
   * Fired after template appears
   * @param e Event
   */
  onDidAppear?(e: BaseEvent): void;
  /**
   * Fired after template disappears
   * @param e Event
   */
  onDidDisappear?(e: BaseEvent): void;

  /**
   * Fired when bar button is pressed
   * @param e Event
   */
  onBarButtonPressed?(e: BarButtonEvent): void;

  /**
   * Fired when popToRootTemplate finished
   */
  onPoppedToRoot?(e: BaseEvent): void;
}

export class Template<P> {
  public get type(): string {
    return 'unset';
  }
  public id!: string;
  public listenerSubscriptions: EmitterSubscription[] = [];

  public get eventMap() {
    return {};
  }

  constructor(public config: TemplateConfig & P) {
    if (config.id) {
      this.id = config.id;
    }

    if (!this.id) {
      this.id = `${this.type}-${Date.now()}-${Math.round(Math.random() * Number.MAX_SAFE_INTEGER)}`;
    }

    const eventMap = {
      barButtonPressed: 'onBarButtonPressed',
      didAppear: 'onDidAppear',
      didDisappear: 'onDidDisappear',
      willAppear: 'onWillAppear',
      willDisappear: 'onWillDisappear',
      poppedToRoot: 'onPoppedToRoot',
      ...(this.eventMap || {}),
    };

    Object.entries(eventMap).forEach(([eventName, callbackName]) => {
      const subscription = CarPlay.emitter.addListener(eventName, e => {
        // stateDidChange is fired by the scene which does not know anything about the template id but affects only MapTemplate
        const isStateChangedEvent = eventName === 'stateDidChange';
        if (isStateChangedEvent) {
          e = e.isVisible;
        }
        const isAlertDismissedEvent = eventName === 'didDismissNavigationAlert' || eventName === 'willShowNavigationAlert';

        const callback =
          (e.templateId === this.id || isStateChangedEvent || isAlertDismissedEvent) && callbackName in config
            ? config[callbackName as keyof typeof config]
            : null;
        if (callback == null || typeof callback !== 'function') {
          return;
        }
        callback(e);
      });

      this.listenerSubscriptions.push(subscription);
    });

    // all these templates call createTemplate on their own
    const types = Object.values<string>(AndroidRenderTemplates).concat(['list', 'signin', 'message']);

    if (!types.includes(this.type)) {
      const callbackFn = Platform.select({
        android: ({ error }: { error?: string } = {}) => {
          error && console.error(error);
        },
      });
      CarPlay.bridge.createTemplate(
        this.id,
        this.parseConfig({ type: this.type, ...config }),
        callbackFn,
      );
    }
  }

  updateTemplate = (config: P) => {
    this.config = this.parseConfig({ type: this.type, ...config });
    CarPlay.bridge.updateTemplate(this.id, this.config);
  };

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  public parseConfig(config: any) {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    function traverse(obj: any) {
      for (const key in obj) {
        if (obj[key] != null && typeof obj[key] === 'object') {
          traverse(obj[key]);
        }
        if (key === 'image' || key === "tabImage") {
          // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
          const resolved = Image.resolveAssetSource(obj[key]);
          obj[key] = {
            ...resolved,
            ...(obj["tintColor"] && obj["tintColor"] != null ? {tintColor: obj["tintColor"]} : {}),
          };
        }
      }
    }
    const result = JSON.parse(JSON.stringify(config));
    traverse(result);
    return result;
  }

  public destroy() {
    this.listenerSubscriptions.forEach(listener => listener.remove());
    this.listenerSubscriptions = [];
  }
}
