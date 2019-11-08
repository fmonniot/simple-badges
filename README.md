# simple-badges
A simple service to render badges similar to shields.io

## Getting Started

You'll need SBT installed and ready to use. Once this is the case, simply run `sbt server/run` from the command line.

## APIs

Once running, simple-badges offer three APIs (at the time of writing):

```
GET /badges/generic/:label/:message
GET /badges/generic/:message
GET /badges/gitlab/:projectId/tags
```

Note: for an exhausive list, look up objets in `server/src/main/scala/eu/monniot/simplebadges/http`
