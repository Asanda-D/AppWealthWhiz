package vcmsa.projects.wealthwhizap

import android.app.Application
import com.google.firebase.FirebaseApp

class WealthWhizApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}