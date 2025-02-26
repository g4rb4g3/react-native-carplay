import { ImageSourcePropType } from 'react-native';
import { Template, TemplateConfig } from './Template';

export interface ContactButtonEvent {
  id: string;
  templateId: string;
}

export interface ContactActionBase {
  id: string;
  type: 'call' | 'directions' | 'message';
  disabled?: boolean;
  title?: string;
}

export interface ContactActionMessage extends ContactActionBase {
  type: 'message';
  phoneOrEmail: string;
}

export type ContactAction = ContactActionBase | ContactActionMessage;

export interface ContactTemplateConfig extends TemplateConfig {
  name: string;
  image: ImageSourcePropType;
  subtitle?: string;
  informativeText?: string;
  actions?: ContactAction[];

  /**
   * Fired when bar button is pressed
   * @param e Event
   */
  onButtonPressed?(e: ContactButtonEvent): void;

  /**
   * Fired when the back button is pressed
   */
  onBackButtonPressed?(): void;

  /**
   * Option to hide back button
   * @default false
   */
  backButtonHidden?: boolean;

  /**
   * Title to be shown on the back button, defaults to no text so only the < icon is shown
   */
  backButtonTitle?: string;
}

export class ContactTemplate extends Template<ContactTemplateConfig> {
  public get type(): string {
    return 'contact';
  }
  get eventMap() {
    return {
      buttonPressed: 'onButtonPressed',
      backButtonPressed: 'onBackButtonPressed',
    };
  }
}
