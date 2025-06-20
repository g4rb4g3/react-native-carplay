import { Image, Platform } from 'react-native';
import { CarPlay } from '../CarPlay';
import { ListItem } from '../interfaces/ListItem';
import { BaseEvent, Template, TemplateConfig } from './Template';
import { Action } from 'src/interfaces/Action';

export interface SearchTemplateConfig extends TemplateConfig {
  /**
   * Fired when search input is changed.
   * Must return list of items to show.
   * @param query Search query
   */
  onSearch?(query: string): Promise<ListItem[]>;
  /**
   * Fired when result item is selected.
   * Spinner shows by default.
   * When the returned promise is resolved the spinner will hide.
   * @param item Object with the selected index
   */
  onItemSelect?(item: { index: number; id: string }): Promise<void>;
  /**
   * Fired when search button is pressed
   */
  onSearchButtonPressed?(e: BaseEvent): void;
  /**
   * Fired when the back button is pressed
   */
  onBackButtonPressed?(): void;

  /**
   * Fired when one of the action buttons is pressed
   * @namespace Android
   */
  onActionButtonPressed?({ buttonId }: { buttonId: string }): void;
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
   * Sets the ActionStrip for this template or null to not display an .
   * This template allows up to 2 Actions. Of the 2 allowed Actions, one of them can contain a title as set via setTitle. Otherwise, only Actions with icons are allowed.
   */
  actions?: [Action<'custom'>] | [Action<'custom'>, Action<'custom'>];
  /**
   * Sets a single ItemList to show in the template.
   * @namespace Android
   */
  items?: ListItem[];
  /**
   * Sets the initial search text
   * @namespace Android
   */
  initialSearchText?: string;
  /**
   * Sets the search hint
   * @namespace Android
   */
  searchHint?: string;
  /**
   * Show keyboard when template is shown
   * @namespace Android
   */
  showKeyboardByDefault?: boolean;
}

export class SearchTemplate extends Template<SearchTemplateConfig> {
  public get type(): string {
    return 'search';
  }

  get eventMap() {
    return {
      searchButtonPressed: 'onSearchButtonPressed',
      backButtonPressed: 'onBackButtonPressed',
    };
  }

  constructor(public config: SearchTemplateConfig) {
    // parse out any images in the results

    super(config);

    let subscription = CarPlay.emitter.addListener(
      'updatedSearchText',
      async (e: { searchText: string; templateId: string }) => {
        if (config.onSearch && e.templateId === this.id) {
          const result = await config.onSearch(e.searchText).catch(() => null);
          if (result == null) {
            return;
          }
          const parsedResults = result.map(item => ({
            ...item,
            image: item.image ? Image.resolveAssetSource(item.image) : undefined,
          }));

          if (Platform.OS === 'ios') {
            CarPlay.bridge.reactToUpdatedSearchText(e.templateId, parsedResults);
          } else if (Platform.OS === 'android') {
            this.config = { ...this.config, items: parsedResults };
            CarPlay.bridge.updateTemplate(
              e.templateId,
              JSON.parse(JSON.stringify({ ...this.config, type: this.type })),
            );
          }
        }
      },
    );

    this.listenerSubscriptions.push(subscription);

    subscription = CarPlay.emitter.addListener(
      'selectedResult',
      (e: { templateId: string; index: number; id: string }) => {
        if (config.onItemSelect && e.templateId === this.id) {
          void Promise.resolve(config.onItemSelect(e)).then(
            () => Platform.OS === 'ios' && CarPlay.bridge.reactToSelectedResult(true),
          );
        }
      },
    );

    this.listenerSubscriptions.push(subscription);

    subscription = CarPlay.emitter.addListener(
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
  }
}
