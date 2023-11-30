package  play.silhouette.helpers

private[silhouette] object Transform {

  implicit class MapOps[K, V](val map: Map[K, V]) extends AnyVal {
    def transformValues[W](f: V => W): Map[K, W] = map.view.mapValues(f).toMap
  }
}
