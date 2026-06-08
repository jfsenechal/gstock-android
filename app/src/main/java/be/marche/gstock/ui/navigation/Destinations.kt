package be.marche.gstock.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Workers("workers", "Workers", Icons.Filled.People),
    Tools("tools", "Tools", Icons.Filled.Build),
    Checkouts("checkouts", "Checkouts", Icons.AutoMirrored.Filled.Assignment),
    Checkout("checkout", "Check out", Icons.Filled.QrCodeScanner),
}
