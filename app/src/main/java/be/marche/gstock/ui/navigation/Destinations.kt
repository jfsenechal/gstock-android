package be.marche.gstock.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.ui.graphics.vector.ImageVector
import be.marche.gstock.R

enum class Destination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Checkouts("checkouts", R.string.nav_checkouts, Icons.AutoMirrored.Filled.Assignment),
    Catalog("catalog", R.string.nav_catalog, Icons.Filled.Inventory2),
    Checkout("checkout", R.string.nav_checkout, Icons.Filled.QrCodeScanner),
    Account("account", R.string.nav_account, Icons.Filled.AccountCircle),
}
