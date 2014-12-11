# voter

to compile css

```bash
sass src/sass/app.scss > resources/public/css/app.css
``````


to watch stylesheets for changes

```bash
sass --watch src/sass:resources/public/css
``````

deployment

```bash
lein distribute staging
lein distribute production
```

Copyright © 2013 John Andrews, Derek Briggs, Neo Innovation

Distributed under the Eclipse Public License, the same as Clojure.
