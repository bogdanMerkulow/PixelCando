package pixel.cando.ui

import androidx.fragment.app.Fragment
import com.github.terrakok.cicerone.androidx.FragmentScreen
import pixel.cando.di.ExamDetailsArgument
import pixel.cando.ui._base.fragment.withArgumentSet
import pixel.cando.ui.auth.AuthFlowFragment
import pixel.cando.ui.auth.password_recovery.PasswordRecoveryFragment
import pixel.cando.ui.auth.sign_in.SignInFragment
import pixel.cando.ui.main.MainFlowFragment
import pixel.cando.ui.main.chat_list.ChatListFragment
import pixel.cando.ui.main.chat_messaging.ChatMessagingFragment
import pixel.cando.ui.main.exam_details.ExamDetailsFragment
import pixel.cando.ui.main.home.HomeFragment
import pixel.cando.ui.main.patient_details.PatientDetailsFragment
import pixel.cando.ui.main.patient_info.PatientInfoFragment
import pixel.cando.ui.main.patient_list.PatientListFragment
import pixel.cando.ui.main.profile.ProfileFragment
import pixel.cando.ui.root.RootFragment
import pixel.cando.ui.splash.SplashFragment

object Screens {

    fun root() = FragmentScreen { RootFragment() }

    fun splash() = FragmentScreen { SplashFragment() }

    fun authFlow() = FragmentScreen { AuthFlowFragment() }
    fun signIn() = FragmentScreen { SignInFragment() }
    fun passwordRecovery(
        email: String
    ) = FragmentScreen {
        PasswordRecoveryFragment()
            .withArgumentSet(email)
    }

    fun mainFlow() = FragmentScreen { MainFlowFragment() }
    fun home() = FragmentScreen { HomeFragment() }

    fun patients() = FragmentScreen { PatientListFragment() }
    fun patientDetails(
        patientId: Long,
    ) = FragmentScreen {
        PatientDetailsFragment()
            .withArgumentSet(patientId)
    }

    fun patientInfo(
        patientId: Long,
    ) = FragmentScreen {
        PatientInfoFragment()
            .withArgumentSet(patientId)
    }

    fun examDetails(
        examId: Long,
        patientId: Long
    ) = FragmentScreen {
        ExamDetailsFragment()
            .withArgumentSet(
                ExamDetailsArgument(
                    examId = examId,
                    patientId = patientId,
                )
            )
    }

    fun profile() = FragmentScreen {
        ProfileFragment()
    }

    fun chatList() = FragmentScreen {
        ChatListFragment()
    }

    fun chatMessaging(
        chatId: Long
    ) = FragmentScreen {
        ChatMessagingFragment()
            .withArgumentSet(chatId)
    }

    fun empty() = FragmentScreen { Fragment() } //only for development

}