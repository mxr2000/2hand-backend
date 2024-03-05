package org.mxr.shop.db

import com.cloudinary.utils.ObjectUtils

object Cloudinary {

  import com.cloudinary._
  val cloudinary = new Cloudinary(
    ObjectUtils.asMap(
      "cloud_name",
      "dfplwulxn",
      "api_key",
      "927151186125346",
      "api_secret",
      "Dmzj9B2UckWDjXNkQ8RBcVvj7I4"
    )
  )
}
