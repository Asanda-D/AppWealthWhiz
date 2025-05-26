package vcmsa.projects.wealthwhizap

import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class FullscreenImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_image)

        val fullscreenImageView = findViewById<ImageView>(R.id.fullscreenImageView)
        val btnClose = findViewById<ImageButton>(R.id.btnClose)

        val imageUri = intent.getStringExtra("imageUri") ?: run {
            finish()
            return
        }

        Glide.with(this)
            .load(Uri.parse(imageUri))
            .into(fullscreenImageView)

        btnClose.setOnClickListener {
            finish()
        }

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }
}