import { GridButton } from '../interfaces/GridButton';
import { BaseEvent, Template, TemplateConfig } from './Template';

export interface ButtonPressedEvent extends BaseEvent {
  /**
   * Button ID
   */
  id: string;
  /**
   * Button Index
   */
  index: number;
  /**
   * template ID
   */
  templateId: string;
}

export interface GridTemplateConfig extends TemplateConfig {
  /**
   * The title displayed in the navigation bar while the list template is visible.
   */
  title?: string;
  /**
   * The array of grid buttons displayed on the template.
   */
  buttons: GridButton[];
  /**
   * Fired when a button is pressed
   */
  onButtonPressed?(e: ButtonPressedEvent): void;
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

export class GridTemplate extends Template<GridTemplateConfig> {
  public get type(): string {
    return 'grid';
  }

  get eventMap() {
    return {
      gridButtonPressed: 'onButtonPressed',
      backButtonPressed: 'onBackButtonPressed',
    };
  }
}
