import { Platform } from 'react-native';
import { CarPlay } from '../CarPlay';
import { Action, CallbackAction, getCallbackActionId } from '../interfaces/Action';
import { ListItem } from '../interfaces/ListItem';
import { ListItemUpdate } from '../interfaces/ListItemUpdate';
import { ListSection } from '../interfaces/ListSection';
import { Template, TemplateConfig } from './Template';

export interface ListTemplateConfig extends TemplateConfig {
  /**
   * The title displayed in the navigation bar while the list template is visible.
   */
  title?: string;
  /**
   * The sections displayed in the list.
   */
  sections?: ListSection[];
  /**
   * Sets a single ItemList to show in the template.
   * @namespace Android
   */
  items?: ListItem[];
  /**
   *  An optional array of strings, ordered from most to least preferred.
   *  The variant strings should be provided as localized, displayable content.
   *  The system will select the first variant that fits the available space.
   *  If the list template does not contain any items (itemCount == 0), then
   *  the template will display an empty view with a title and subtitle to indicate
   *  that the template has no list items.
   *  If the list template is updated to contain items, the empty view will be automatically
   *  removed.
   * @namespace iOS
   */
  emptyViewTitleVariants?: string[];
  /**
   *  An optional array of strings, ordered from most to least preferred.
   *  The variant strings should be provided as localized, displayable content.
   *  The system will select the first variant that fits the available space.
   *  If the list template does not contain any items (itemCount == 0), then
   *  the template will display an empty view with a title and subtitle to indicate
   *  that the template has no list items.
   *  If the list template is updated to contain items, the empty view will be automatically
   *  removed.
   *  @namespace iOS
   */
  emptyViewSubtitleVariants?: string[];
  /**
   * Fired when list item is selected.
   * Spinner shows by default.
   * When the returned promise is resolved the spinner will hide.
   * @param item Object with the selected index
   */
  onItemSelect?(item: { templateId: string; index: number; id: ListItem["id"] }): Promise<void>;

  /**
   * Fired when image row item is selected.
   * Spinner shows by default.
   * When the returned promise is resolved the spinner will hide.
   * @param item Object with the selected index
   */
  onImageRowItemSelect?(item: {
    templateId: string;
    index: number;
    imageIndex: number;
  }): Promise<void>;

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
   * Assistant Configuration
   * @see https://developer.apple.com/documentation/carplay/cplisttemplate#3762508
   * @namespace iOS
   */
  assistant?: {
    enabled: boolean;
    position: 'top' | 'bottom';
    visibility: 'off' | 'always' | 'limited';
    action: 'playMedia' | 'startCall';
  };
  /**
   * Sets whether the template is in a loading state.
   * If set to true, the UI will display a loading indicator where the list content would be otherwise. The caller is expected to call invalidate and send the new template content to the host once the data is ready.
   * If set to false, the UI will display the contents of the ItemList instance(s) added via setSingleList or addSectionedList.
   * @namespace Android
   */
  loading?: boolean;
  /**
   * Sets the Action that will be displayed in the header of the template.
   * @namespace Android
   */
  headerAction?: Action<'appIcon' | 'back'>;
  /**
   * Sets the ActionStrip for this template or null to not display any.
   * This template allows up to 2 Actions. Of the 2 allowed Actions, one of them can contain a title as set via setTitle. Otherwise, only Actions with icons are allowed.
   */
  actions?:
    | [CallbackAction | Action<'appIcon' | 'custom'>]
    | [
        CallbackAction | Action<'appIcon' | 'custom'>,
        CallbackAction | Action<'appIcon' | 'custom'>,
      ];
}

/**
 * A hierarchical list of menu items can be displayed on the CarPlay screen using a list template.
 *
 * The List Template allows navigation apps to present a hierarchical list of menu items. It includes a navigation bar and a list view.
 *
 * The navigation bar includes a title, and up to two (2) leading buttons and two (2) trailing buttons. You can customize the appearance of these buttons with icons or text.
 *
 * Each item in the list view may include an icon, title, subtitle, and an optional disclosure indicator indicating the presence of a submenu. The depth of the menu hierarchy may not exceed 5 levels. Note that some cars limit the total number of items that may be shown in a list.
 */
export class ListTemplate extends Template<ListTemplateConfig> {
  public get type(): string {
    return 'list';
  }

  get eventMap() {
    return {
      backButtonPressed: 'onBackButtonPressed',
    };
  }

  private pressableCallbacks: {
    [key: string]: () => void;
  } = {};

  constructor(public config: ListTemplateConfig) {
    super(config);

    let subscription = CarPlay.emitter.addListener(
      'didSelectListItem',
      (e: { templateId: string; index: number; id: string }) => {
        if (config.onItemSelect && e.templateId === this.id) {
          void Promise.resolve(config.onItemSelect(e)).then(() => {
            if (Platform.OS === 'ios') {
              CarPlay.bridge.reactToSelectedResult(true);
            }
          });
        }
      },
    );

    this.listenerSubscriptions.push(subscription);

    subscription = CarPlay.emitter.addListener(
      'didSelectListItemRowImage',
      (e: { templateId: string; index: number; imageIndex: number }) => {
        if (config.onImageRowItemSelect && e.templateId === this.id) {
          void Promise.resolve(config.onImageRowItemSelect(e)).then(() => {
            if (Platform.OS === 'ios') {
              CarPlay.bridge.reactToSelectedResult(true);
            }
          });
        }
      },
    );

    this.listenerSubscriptions.push(subscription);

    subscription = CarPlay.emitter.addListener(
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
    const updatedConfig: Omit<ListTemplateConfig, 'actions'> & { actions?: Array<Action> } = rest;
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

  public updateSections = (sections: ListSection[]) => {
    this.config = { ...this.config, sections };
    return CarPlay.bridge.updateListTemplateSections(this.id, super.parseConfig(sections));
  };

  public updateListTemplateItem = (config: ListItemUpdate) => {
    const { itemIndex: updateIndex, sectionIndex: updateSection, ...update } = config;
    this.config = {
      ...this.config,
      sections: this.config.sections?.map((section, index) =>
        index === updateSection
          ? {
              ...section,
              items: section.items.map((item, itemIndex) =>
                itemIndex === updateIndex ? update : item,
              ),
            }
          : section,
      ),
    };
    return CarPlay.bridge.updateListTemplateItem(this.id, super.parseConfig(config));
  };

  public getMaximumListItemCount() {
    return CarPlay.bridge.getMaximumListItemCount(this.id);
  }

  public getMaximumListSectionCount() {
    return CarPlay.bridge.getMaximumListSectionCount(this.id);
  }

  public getMaximumListItemImageSize() {
    return CarPlay.bridge.getMaximumListItemImageSize(this.id);
  }

  public getMaximumNumberOfGridImages() {
    return CarPlay.bridge.getMaximumNumberOfGridImages(this.id);
  }

  public getMaximumListImageRowItemImageSize() {
    return CarPlay.bridge.getMaximumListImageRowItemImageSize(this.id);
  }
}
