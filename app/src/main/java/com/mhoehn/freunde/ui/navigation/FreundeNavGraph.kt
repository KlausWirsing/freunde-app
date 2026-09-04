package com.mhoehn.freunde.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mhoehn.freunde.ui.LocalAppContainer
import com.mhoehn.freunde.ui.screens.detail.PersonDetailScreen
import com.mhoehn.freunde.ui.screens.list.PersonListScreen
import com.mhoehn.freunde.ui.screens.login.LoginScreen
import com.mhoehn.freunde.ui.screens.meetingform.MeetingFormScreen
import com.mhoehn.freunde.ui.screens.personform.PersonFormScreen
import com.mhoehn.freunde.ui.screens.settings.SettingsScreen

object Routes {
    const val LOGIN = "login"
    const val PERSON_LIST = "personList"
    const val PERSON_DETAIL = "personDetail/{personId}"
    const val PERSON_FORM = "personForm?personId={personId}"
    const val MEETING_FORM = "meetingForm/{personId}?meetingId={meetingId}"
    const val SETTINGS = "settings"

    fun personDetail(id: String) = "personDetail/$id"
    fun personFormNew() = "personForm"
    fun personFormEdit(id: String) = "personForm?personId=$id"
    fun meetingFormNew(personId: String) = "meetingForm/$personId"
    fun meetingFormEdit(personId: String, meetingId: String) = "meetingForm/$personId?meetingId=$meetingId"
}

@Composable
fun FreundeNavGraph(
    navController: NavHostController = rememberNavController(),
    pendingPersonId: String? = null
) {
    val container = LocalAppContainer.current
    val currentUser by container.authRepository.authState.collectAsState(initial = container.authRepository.currentUser)
    var unconsumedPendingPersonId by remember { mutableStateOf(pendingPersonId) }

    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen()
        }

        composable(Routes.PERSON_LIST) {
            PersonListScreen(
                onPersonClick = { id -> navController.navigate(Routes.personDetail(id)) },
                onAddPerson = { navController.navigate(Routes.personFormNew()) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            route = Routes.PERSON_DETAIL,
            arguments = listOf(navArgument("personId") { type = NavType.StringType })
        ) { backStackEntry ->
            val personId = backStackEntry.arguments?.getString("personId") ?: return@composable
            PersonDetailScreen(
                personId = personId,
                onBack = { navController.popBackStack() },
                onEditPerson = { id -> navController.navigate(Routes.personFormEdit(id)) },
                onAddMeeting = { id -> navController.navigate(Routes.meetingFormNew(id)) },
                onEditMeeting = { pId, mId -> navController.navigate(Routes.meetingFormEdit(pId, mId)) }
            )
        }

        composable(
            route = Routes.PERSON_FORM,
            arguments = listOf(
                navArgument("personId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val personId = backStackEntry.arguments?.getString("personId")
            PersonFormScreen(
                personId = personId,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.MEETING_FORM,
            arguments = listOf(
                navArgument("personId") { type = NavType.StringType },
                navArgument("meetingId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val personId = backStackEntry.arguments?.getString("personId") ?: return@composable
            val meetingId = backStackEntry.arguments?.getString("meetingId")
            MeetingFormScreen(
                personId = personId,
                meetingId = meetingId,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onSignedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }

    // Auth-Status ist die Quelle der Wahrheit für Login vs. Personenliste;
    // hier statt eines dynamischen startDestination reaktiv umgeleitet.
    LaunchedEffect(currentUser) {
        val destination = navController.currentDestination?.route
        if (currentUser != null && destination == Routes.LOGIN) {
            navController.navigate(Routes.PERSON_LIST) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        } else if (currentUser == null && destination != null && destination != Routes.LOGIN) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }

        val personId = unconsumedPendingPersonId
        if (currentUser != null && personId != null) {
            navController.navigate(Routes.personDetail(personId))
            unconsumedPendingPersonId = null
        }
    }
}
