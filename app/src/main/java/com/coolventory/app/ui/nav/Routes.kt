package com.coolventory.app.ui.nav

/** Central route table. Keeps argument names and route construction in one place. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val SEARCH = "search"
    const val HISTORY = "history"
    const val BUY_AGAIN = "buy_again"
    const val SETTINGS = "settings"

    const val ALL_PRODUCTS = "all_products"
    const val EXPIRY_REVIEW = "expiry_review"
    const val STATISTICS = "statistics"
    const val SHELF_MANAGEMENT = "shelf_management"

    const val ARG_PRODUCT_ID = "productId"
    const val ARG_EVENT_ID = "eventId"
    const val ARG_LOCATION = "location"
    const val ARG_SHELF_ID = "shelfId"

    // Add product accepts optional preset location + shelf.
    const val ADD_PRODUCT_PATTERN = "add_product?$ARG_LOCATION={$ARG_LOCATION}&$ARG_SHELF_ID={$ARG_SHELF_ID}"
    fun addProduct(location: String? = null, shelfId: String? = null): String =
        "add_product?$ARG_LOCATION=${location ?: ""}&$ARG_SHELF_ID=${shelfId ?: ""}"

    const val EDIT_PRODUCT_PATTERN = "edit_product/{$ARG_PRODUCT_ID}"
    fun editProduct(productId: String): String = "edit_product/$productId"

    const val PRODUCT_DETAIL_PATTERN = "product_detail/{$ARG_PRODUCT_ID}"
    fun productDetail(productId: String): String = "product_detail/$productId"

    const val HISTORY_DETAIL_PATTERN = "history_detail/{$ARG_EVENT_ID}"
    fun historyDetail(eventId: String): String = "history_detail/$eventId"

    /** Destinations that show the bottom navigation bar. */
    val bottomBarRoutes = setOf(DASHBOARD, SEARCH, HISTORY, BUY_AGAIN, SETTINGS)
}
