package com.example.model

import com.example.R

object SketchTemplates {
  val defaultTemplates = listOf(
    SketchItem(
      id = "template_butterfly",
      title = "Butterfly",
      category = "Animals",
      drawableResId = R.drawable.ic_sketch_butterfly,
      isCustomPhoto = false
    ),
    SketchItem(
      id = "template_cat",
      title = "Cute Kitty",
      category = "Animals",
      drawableResId = R.drawable.ic_sketch_cat,
      isCustomPhoto = false
    ),
    SketchItem(
      id = "template_rose",
      title = "Rose Flower",
      category = "Nature",
      drawableResId = R.drawable.ic_sketch_rose,
      isCustomPhoto = false
    ),
    SketchItem(
      id = "template_anime",
      title = "Anime Girl",
      category = "Anime",
      drawableResId = R.drawable.ic_sketch_anime,
      isCustomPhoto = false
    )
  )
}
