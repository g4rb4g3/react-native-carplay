import { Action, CallbackAction, HeaderAction, getCallbackActionId } from '../../interfaces/Action';
import { Template, TemplateConfig } from '../Template';
import { CarPlay } from '../../CarPlay';

type SignInMethodQr = {
  type: 'qr';
  /**
   * url that is placed in the QR code
   */
  url?: string;
};

type SignInMethodGoogle = {
  /**
   * make sure to check CarPlay.getPlayServicesAvailable first
   * in case play services are not available this will not work
   */
  type: 'google';
  serverClientId: string;
  /**
   * title that is shown on the button, something like "Sign in with Google"
   */
  actionTitle: string;
};

export enum InputType {
  DEFAULT = 1,
  PASSWORD = 2,
}

type SignInMethodMail = {
  type: 'mail';
  /**
   * Sets the text explaining to the user what should be entered in this input box.
   */
  hint: string;
  inputType: InputType;
};

export interface SignInTemplateConfig extends TemplateConfig {
  headerAction?: HeaderAction;
  /**
   * Sets the title of the template.
   */
  title: string;

  /**
   * Sets the text to show as instructions of the template.
   */
  instructions: string;

  /**
   * Sets additional text, such as disclaimers, links to terms of services, to show in the template.
   */
  additionalText?: string;

  /**
   * Sign in method to use for this template
   */
  method: SignInMethodQr | SignInMethodMail | SignInMethodGoogle;

  actions?: [CallbackAction] | [CallbackAction, CallbackAction];

  /**
   * Fired when the back button is pressed
   */
  onBackButtonPressed?(): void;

  /**
   * callback that is triggered when user logged in with mail & password or Google
   * this is not triggered for QR sign in so it is optional
   * @param serverAuthCode returned when the user logged in with Google
   * @param text returned when either email or password are submitted by the user
   */
  onSignIn?(e: { templateId: string; serverAuthCode?: string; text?: string }): void;
}

export class SignInTemplate extends Template<SignInTemplateConfig> {
  public get type(): string {
    return 'signin';
  }

  private pressableCallbacks: {
    [key: string]: () => void;
  } = {};

  constructor(public config: SignInTemplateConfig) {
    super(config);

    const subscription = CarPlay.emitter.addListener(
      'buttonPressed',
      ({ buttonId, templateId }: { templateId?: string; buttonId: string }) => {
        if (templateId !== this.id) {
          return;
        }

        const callback = this.pressableCallbacks[buttonId];
        if (callback == null || typeof callback !== 'function') {
          return;
        }
        callback();
      },
    );

    this.listenerSubscriptions.push(subscription);

    const callbackFn = ({ error }: { error?: string } = {}) => {
      error && console.error(error);
    };

    this.config = this.parseConfig({ type: this.type, ...config });
    CarPlay.bridge.createTemplate(this.id, this.config, callbackFn);
  }

  public parseConfig(config: any) {
    const { actions, ...rest }: { actions: Array<CallbackAction | Action> } = config;
    const updatedConfig: Omit<SignInTemplateConfig, 'actions'> & { actions?: Array<Action> } =
      rest as SignInTemplateConfig;
    const callbackIds: Array<string> = [];

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

    this.pressableCallbacks = Object.fromEntries(
      Object.entries(this.pressableCallbacks).filter(([id]) => callbackIds.includes(id)),
    );

    return super.parseConfig(updatedConfig);
  }

  get eventMap() {
    return {
      backButtonPressed: 'onBackButtonPressed',
      didSignIn: 'onSignIn',
    };
  }
}
