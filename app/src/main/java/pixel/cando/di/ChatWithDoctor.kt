package pixel.cando.di

import pixel.cando.ui.main.chat_messaging.ChatMessagingFragment
import pixel.cando.ui.main.chat_with_doctor.ChatWithDoctorFragment

fun ChatWithDoctorFragment.setup(
) {
    if (contentFragmentProvider != null) {
        return
    }
    contentFragmentProvider = {
        ChatMessagingFragment()
    }
}