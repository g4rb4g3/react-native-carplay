import { CarPlay } from '../../CarPlay';
import { Action, CallbackAction, HeaderAction, getCallbackActionId } from '../../interfaces/Action';
import { Template, TemplateConfig } from '../Template';
import { ImageSourcePropType, Platform } from 'react-native';

export interface MessageTemplateConfig extends TemplateConfig {
  message?: string;
  loading?: boolean;
  headerAction?: HeaderAction;
  actions?: [CallbackAction, CallbackAction] | [CallbackAction];
  image?: ImageSourcePropType;
  title?: string;
  debugMessage?: string;
}

export class MessageTemplate extends Template<MessageTemplateConfig> {
  private pressableCallbacks: {
    [key: string]: () => void;
  } = {};

  public get type(): string {
    return 'message';
  }

  constructor(public config: MessageTemplateConfig) {
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

    const callbackFn = Platform.select({
      android: ({ error }: { error?: string } = {}) => {
        error && console.error(error);
      },
    });

    this.config = this.parseConfig({ type: this.type, ...config });
    CarPlay.bridge.createTemplate(this.id, this.config, callbackFn);
  }

  public parseConfig(config: any) {
    const { actions, ...rest }: { actions: Array<CallbackAction | Action<'appIcon' | 'custom'>> } =
      config;
    const updatedConfig: Omit<MessageTemplateConfig, 'actions'> & { actions?: Array<Action> } =
      rest;
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
}
