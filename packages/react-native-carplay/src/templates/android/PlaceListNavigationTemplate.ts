import { AndroidRenderTemplates } from '../../interfaces/AndroidRenderTemplates';
import { AndroidAction } from '../../interfaces/Action';
import { Header } from '../../interfaces/Header';
import { ListItem } from '../../interfaces/ListItem';
import {
  AndroidNavigationBaseTemplate,
  AndroidNavigationBaseTemplateConfig,
} from './AndroidNavigationBaseTemplate';

export interface PlaceListNavigationTemplateConfig extends AndroidNavigationBaseTemplateConfig {
  /**
   * Sets an ActionStrip with a list of template-scoped actions for this template.
   * The Action buttons in Map Based Template are automatically adjusted based on the screen size. On narrow width screen, icon Actions show by default. If no icon specify, showing title Actions instead. On wider width screen, title Actions show by default. If no title specify, showing icon Actions instead.
   * @limit This template allows up to 4 Actions in its ActionStrip. Of the 4 allowed Actions, it can either be a title Action as set via setTitle, or a icon Action as set via setIcon.
   */
  actions?: Array<AndroidAction>;
  /**
   * Sets the Header for this template.
   */
  header?: Header;
  /**
   * Sets an ItemList to show in a list view along with the map.
   * Unless set with this method, the template will not show an item list.
   * To show a marker corresponding to a point of interest represented by a row, set the Place instance via setMetadata. The host will display the PlaceMarker in both the map and the list view as the row becomes visible.
   * @limit The number of items in the ItemList should be smaller or equal than the limit provided by CONTENT_LIMIT_TYPE_PLACE_LIST. The host will ignore any items over that limit. The list itself cannot be selectable as set via setOnSelectedListener. Each Row can add up to 2 lines of texts via addText and cannot contain a Toggle.
   */
  itemList?: ListItem[];
  /**
   * Sets whether the template is in a loading state.
   */
  loading?: boolean;
  /**
   * Sets an ActionStrip with a list of map-control related actions for this template, such as pan or zoom.
   * The host will draw the buttons in an area that is associated with map controls.
   * If the app does not include the PAN button in this ActionStrip, the app will not receive the user input for panning gestures from SurfaceCallback methods, and the host will exit any previously activated pan mode.
   * @limit This template allows up to 4 Actions in its map ActionStrip. Only Actions with icons set via setIcon are allowed.
   */
  mapButtons?: Array<AndroidAction>;
  /**
   * Title for the map
   */
  title?: string;
}

/**
 * A template that supports showing a list of places alongside a custom drawn map.
 * The template itself does not expose a drawing surface. In order to draw on the canvas, use setSurfaceCallback.
 * Template Restrictions In regards to template refreshes, as described in onGetTemplate, this template is considered a refresh of a previous one if:
 * - The previous template is in a loading state (see setLoading, or
 * - The template title has not changed, and the number of rows and the title (not counting spans) of each row between the previous and new ItemLists have not changed.
 * - The template is sent in response to a user-initiated content refresh request. (see setOnContentRefreshListener.
 * In order to use this template your car app MUST declare that it uses the **androidx.car.app.NAVIGATION_TEMPLATES** permission in the manifest.
 */
export class PlaceListNavigationTemplate extends AndroidNavigationBaseTemplate<PlaceListNavigationTemplateConfig> {
  public get type(): string {
    return AndroidRenderTemplates.PlaceNavigationMap;
  }
}
