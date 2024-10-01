# Web Development guidance for the Integration Hub

## CSS
When copying CSS rules from the prototype bear in mind that the CSS you see in the browser is the output of the 
prototype's CSS preprocessor, not [the original source code](https://github.com/hmrc/integration-hub-prototype/blob/main/app/assets/sass/application.scss) written by the prototype's developers. As such the in-browser CSS may not exemplify best practices and it may be possible to write the rules in a more maintainable way when including 
them in the application.scss file. 

Some guidelines when adding new CSS rules:

### Class names
Prefix all class names with `hip-` and use [BEM naming conventions](https://getbem.com/naming/).

For example, rather than
```html
<div class="apiFilterActive"> 
```
use
```html
<div class="hip-api-filter--active"> 
```

### Scoping
SCSS allows you to [nest rules](https://sass-lang.com/documentation/style-rules/#nesting) within other rules. We should take
advantage of this in order to improve the specificity of selectors and group related rules together within the application.scss file.

Example:
```scss
.hip-stat-container {
    display: flex;

    .hip-stat-item {
        background-color: $off-white;

        .hip-stat-title {
            @include govuk-font($size: 14);
        }

        .hip-stat-number {
            @include govuk-font($size: 24, $weight: bold);
        }
    }
}
```

### Colours
When specifying a colour use one of the variables defined at the top of the application.scss file. 

For example rather than
```scss
color: #003078;
```
use
```scss
color: $active-link-color;
```

To find out the hex code for a given colour see the [_colours-palette.scss](https://github.com/alphagov/govuk-frontend/blob/main/packages/govuk-frontend/src/govuk/settings/_colours-palette.scss#L14) file or look at the [design system guidance](https://design-system.service.gov.uk/styles/colour/) page.

### Spacing
When specifying padding or margins use the `govuk-spacing()` function.

For example rather than
```scss
margin-top: 30px;
```
use
```scss
margin-top: govuk-spacing(6);
```

To find out what spacing value corresponds to a given pixel value, see the [_spacing.scss](https://github.com/alphagov/govuk-frontend/blob/main/packages/govuk-frontend/src/govuk/settings/_spacing.scss#L11) file or look at the [design system guidance](https://design-system.service.gov.uk/styles/spacing/#static-spacing) page.
