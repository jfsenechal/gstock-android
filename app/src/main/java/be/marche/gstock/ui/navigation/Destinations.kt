package be.marche.gstock.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Checkout("checkout", "Check out", Icons.Filled.QrCodeScanner),
    Catalog("catalog", "Catalog", Icons.Filled.Inventory2),
    Checkouts("checkouts", "Checkouts", Icons.AutoMirrored.Filled.Assignment),
    Account("account", "Account", Icons.Filled.AccountCircle),
}
