package com.collect22allfootball

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.media.ExifInterface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.random.Random

class PuzzleActivity : AppCompatActivity() {

    var pieces:ArrayList<PuzzlePiece>?=null
    var mCurrentPhotopath:String? = null
    var mCurrentPhotoUri:String? =null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_puzzle)

        val layout = findViewById<RelativeLayout>(R.id.layout)

        val imageView= findViewById<ImageView>(R.id.imageView)
            val intent = intent
        val assetName= intent.getStringExtra("assetName")
        mCurrentPhotopath= intent.getStringExtra("mCurrentphotopath")
        mCurrentPhotoUri = intent.getStringExtra("mCurrentPhotoUri")

        //run image related code after the view was laid out
        //to have all dimensuion calculated
        imageView.post {
            if (assetName!=null){
                setPicFromAsset(assetName,imageView)
        }
            else if(mCurrentPhotopath !=null){
                setPicFromPhotoPath(mCurrentPhotopath!!,imageView)
            }
            else if (mCurrentPhotoUri != null){
                imageView.setImageURI(Uri.parse(mCurrentPhotoUri))
            }

            pieces = splitImage()
            val touchListener =TouchListener(this@PuzzleActivity)
            //shuffle pieces order
            Collections.shuffle(pieces)
            for (piece in pieces!!){
                piece.setOnTouchListener(touchListener)
                layout.addView(piece)
                //randomize positon , on the bottom of the screen
                val lParams= piece.layoutParams as RelativeLayout.LayoutParams
                lParams.leftMargin= Random.nextInt(layout.width-piece.pieceWidth)
                lParams.topMargin = layout.height - piece.pieceHeight

                piece.layoutParams  =lParams
            }

        }

    }


    private fun setPicFromAsset(assetName: String, imageView: ImageView?) {

        val targetW = imageView!!.width
        val targetH = imageView.height
        val am = assets
        try {
            val `is` =am.open("img/$assetName")
            //Get the dimension of the bitmap
            val bmOPtion = BitmapFactory.Options()
            BitmapFactory.decodeStream(`is`, Rect (-1,-1,-1,-1),bmOPtion)

            val photoW = bmOPtion.outWidth
            val photoH = bmOPtion.outHeight

            //Determine how much to scale down the image
            val scalFctor = Math.min(
                photoW/targetW,photoH/targetH
            )

            // Decode the image file  into a bitmap sized to  fill the view
            bmOPtion.inJustDecodeBounds = false
            bmOPtion.inSampleSize = scalFctor
            bmOPtion.inPurgeable = true


            val bitmap = BitmapFactory.decodeStream(`is`, Rect(-1,-1,-1,-1),bmOPtion)

            imageView.setImageBitmap(bitmap )

        }catch (e:IOException){
            e.printStackTrace()
            Toast.makeText(this@PuzzleActivity,e.localizedMessage,Toast.LENGTH_SHORT).show()
        }
    }
    private fun splitImage():ArrayList<PuzzlePiece>{
        val piecesNumber = 12
        val  rows = 4
        val cols = 3
        val imageView = findViewById<ImageView>(R.id.imageView)
        val pieces = ArrayList<PuzzlePiece>(piecesNumber)
        // Get  the  scaled  bitmap of the source  image
        val drawable = imageView.drawable as BitmapDrawable
        val bitmap  = drawable.bitmap
        val dimensions = getBitmapPositionInsideImageView(imageView)
        val scaledBitmapLeft = dimensions[0]
        val scaleBitmaptop = dimensions[1]
        val scaleBitmapWidth = dimensions[2]
        Log.d("TAG", "splitImage: " + scaleBitmapWidth)
        val scaleBitmapHeight = dimensions[3]
        val croppedImageWidth = scaleBitmapWidth  - 2 * Math.abs(scaledBitmapLeft)
        val croppedImageHeight = scaleBitmapHeight - 2 * Math.abs(scaleBitmaptop)
        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,scaleBitmapWidth,scaleBitmapHeight,true
        )
        val croppedBitmap = Bitmap.createBitmap(
            scaledBitmap,
            Math.abs(scaledBitmapLeft),
            Math.abs(scaleBitmaptop),croppedImageWidth,croppedImageHeight
        )
        //Calcute the  width and height of the pieces
        val pieceWidth = croppedImageWidth/cols
        val pieceHeight = croppedImageHeight/rows
        //create each bitmap pices and it to the resulting array
        var ycoord = 0
        for (row in 0  until rows) {
            var xcoord = 0
            for (col in 0 until cols) {


                //calculate offset for each piece
                var offsetX = 0
                var offsetY = 0
                if (col>0){
                    offsetX = pieceWidth / 3

                }
                if (row>0){
                    offsetY = pieceHeight / 3
                }
                val pieceBitmap = Bitmap .createBitmap(
                    croppedBitmap,xcoord -offsetX, ycoord - offsetY,
                    pieceWidth + offsetX ,pieceHeight + offsetY

                )
                val piece = PuzzlePiece(applicationContext)
                    piece.setImageBitmap(pieceBitmap)

                piece.xCoord    = xcoord - offsetX +    imageView.left
                piece.yCoord = ycoord - offsetY + imageView.top

                piece.pieceWidth = pieceWidth + offsetX
                piece.pieceHeight = pieceHeight + offsetY

                //this bitmap will hold our final puzzle  pices image
                val puzzlePiece = Bitmap.createBitmap(
                    pieceWidth + offsetX , pieceHeight + offsetY,
                    Bitmap.Config.ARGB_8888
                )

                //Draw  path
                val bumpSize = pieceHeight / 4
                val canvas = Canvas(puzzlePiece)
                val path =  Path()
                path.moveTo(offsetX.toFloat(),offsetY.toFloat())

                if (row == 0){
                    //top side piece
                    path .lineTo(
                        pieceBitmap.width.toFloat(),
                        offsetY.toFloat()
                    )
                }

                else{
                    //top bump
                    path.lineTo(
                        (offsetX +(pieceBitmap.width - offsetX) /3).toFloat(),
                        offsetY.toFloat()
                    )
                    path.cubicTo(
                        (offsetX + (pieceBitmap.width - offsetX).toFloat()),
                        (offsetY - bumpSize).toFloat(),
                        (offsetX + (pieceBitmap.width - offsetX)/ 6*5).toFloat(),
                        (offsetY - bumpSize).toFloat(),
                        (offsetX + (pieceBitmap.width - offsetX )/  3*2).toFloat(),
                        offsetY.toFloat()

                    )
                    path.lineTo(pieceBitmap.width.toFloat(),offsetY.toFloat())
                }


                if (col == cols - 1){
                        // Right side pieces

                    path.lineTo(
                        pieceBitmap.width.toFloat(),
                        pieceBitmap.height.toFloat()
                    )
                }
                else{

                    path.lineTo(
                        pieceBitmap.width.toFloat(),
                        (offsetY + ( pieceBitmap.height - offsetY) /3).toFloat()
                    )
                    path.cubicTo(
//

                        (pieceBitmap.width-bumpSize).toFloat(),
                        (offsetY + (pieceBitmap.height - offsetY) /6).toFloat(),
                        (pieceBitmap.width - bumpSize).toFloat(),
                        (offsetY + (pieceBitmap.height - offsetY) /6 * 5).toFloat(),
                        pieceBitmap.width.toFloat(),
                        (offsetY +( pieceBitmap.height - offsetY) / 3*2).toFloat(),

                    )
                    path.lineTo(

                        pieceBitmap.width.toFloat(),
                        pieceBitmap.height.toFloat()
                    )
                }

                if (row == -1){
                    // Bottom side piece
                    path.lineTo(
                        offsetX.toFloat(),pieceBitmap.height.toFloat()
                    )
                }
                else{
                    // bottom  bump
                    path.lineTo(
                        (offsetX + (pieceBitmap.width) /3 *2).toFloat(),
                        pieceBitmap.height.toFloat()
                    )

                    path.cubicTo(
                        (offsetX + (pieceBitmap.width - offsetX )/ 6*5).toFloat(),
                        (pieceBitmap.height - bumpSize).toFloat(),
                        (offsetX + (pieceBitmap.width - offsetX)/6).toFloat(),
                        (pieceBitmap.height - bumpSize).toFloat(),
                        (offsetX + (pieceBitmap.width -offsetX)/3).toFloat(),
                        pieceBitmap.height.toFloat()
                    )
                    path.lineTo(
                        offsetX.toFloat(),
                        pieceBitmap.height.toFloat()
                    )
                }
                if (col == 0 ){
                    //left side piece
                    path.close()
                }
                else{
                    //left bump
                        path.lineTo(
                            offsetX.toFloat(),
                            (offsetY + (pieceBitmap.height - offsetY)/3*2).toFloat()
                        )

                    path.cubicTo(
                        (offsetX - bumpSize).toFloat(),
                            (offsetY + (pieceBitmap.height)/6*5).toFloat(),
                            (offsetX - bumpSize).toFloat(),
                            (offsetY + (pieceBitmap.height -offsetY)/6).toFloat(),
                        offsetX.toFloat(),
                        (offsetY + (pieceBitmap.height - offsetY)/3).toFloat()

                    )

                    path.close()
                }

                //mask the piese
                val paint  = Paint()
                paint.color = 0x10000000
                paint.style = Paint .Style.FILL
                canvas.drawPath(path,paint)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                canvas.drawBitmap(pieceBitmap,0f,0f,paint)

                // Draw a white border
                 var border = Paint()
                border.color = -0x7f000001
                border.style = Paint.Style.STROKE
                border.strokeWidth = 8.0f
                canvas.drawPath(path,border)

                //draw a black border
                border = Paint()
                border.color = -0x80000000
                border.style = Paint.Style.STROKE
                border.strokeWidth = 3.0f
                canvas.drawPath(path,border)

                // set  the resulting bitmap to the piece
                piece.setImageBitmap(puzzlePiece)
                pieces.add(piece)
                xcoord += pieceWidth


            }
            
            ycoord += pieceHeight
        }
        return pieces
    }

    fun checkGameOver(){
        if (isGameOver){
            AlertDialog.Builder(this@PuzzleActivity)
                .setTitle("You Won....!!")
                .setMessage("You are Win .. \n if you want a new game  ??")
                .setPositiveButton("Yes"){
                    dialog,_->
                    finish()
                    dialog.dismiss()
                }
                .setNegativeButton("No"){
                        dialog,_->
                    finish()
                    dialog.dismiss()
                }
                .create()
                .show()


        }
    }

    private  val isGameOver :Boolean
    private get() {
        for (piece in pieces!!){
            if (piece.canMove){

            }
        }
        return true
    }

    private fun getBitmapPositionInsideImageView(imageView: ImageView?): IntArray {

        val ret = IntArray(4)
        if (imageView == null || imageView.drawable == null){
            return ret
        }
        //get image  dimensions
        //get image  matrix  values  and  place them  in an array

        val f = FloatArray(9)
        imageView.imageMatrix.getValues(f)

            // Extract the scale  values  using   the constants (if  aspect  ratio maintained scaleX == scale)

        val scaleX = f[Matrix.MSCALE_X]
        val scaleY = f[Matrix.MSCALE_Y]

        //get  the drawable  (could aslo  get the bitmap the drawable and getWidth/ getHeight)
        val d = imageView.drawable
        val origW = d.intrinsicWidth
        val origH = d.intrinsicHeight

        //calculate the actual dimensions

        val actW = Math.round(origW * scaleX)
        val actH = Math.round(origH * scaleY)

        ret[2] = actW
        ret[3] = actH

        //Get  image position
        // we  assume  that the  image  is  centred into  imageView

        val imageViewW = imageView.width
        val imageViewH = imageView.height
        val top = (imageViewH - actH)/2
        val left = (imageViewW - actW)/2
        ret[0] = top
        ret[1] = left

        return ret
    }
    private fun setPicFromPhotoPath(mCurrentPhotopath: String, imageView: ImageView?) {

        // get  the dimensions of  the view
        val targetW = imageView!!.width
        val targetH = imageView!!.height


        // get  the dimension of  the bitmap
        val bmOptions = BitmapFactory.Options()


        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(mCurrentPhotopath,bmOptions)


        val  photoW = bmOptions.outWidth
        val photoH = bmOptions.outHeight


        //Determine  how  much to scale  down the image

        val scaleFactor = Math.min(
            photoW/targetW,photoH/targetH
        )

        //decode  the image  file  into  a Bitmap sized to fill the view
        bmOptions.inJustDecodeBounds = false
        bmOptions.inSampleSize = scaleFactor
        bmOptions.inPurgeable = true
        val bitmap = BitmapFactory.decodeFile(mCurrentPhotopath,bmOptions)

        var rotatedBitmap = bitmap


            //rotate bitmap  if needed

        try {
            val ei = ExifInterface(mCurrentPhotopath)
            val orientation = ei.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )

            when(orientation){
                ExifInterface.ORIENTATION_ROTATE_90 -> {
                    rotatedBitmap = rotateImage(bitmap,90f)
                }

                ExifInterface.ORIENTATION_ROTATE_180 -> {
                    rotatedBitmap = rotateImage(bitmap,180f)
                }

                ExifInterface.ORIENTATION_ROTATE_270 -> {
                    rotatedBitmap = rotateImage(bitmap,270f)
                }

            }



        }catch (e:IOException){
            e.printStackTrace()
            Toast.makeText(this@PuzzleActivity,e.localizedMessage, Toast.LENGTH_SHORT     ).show()
        }

        imageView.setImageBitmap(rotatedBitmap)

    }


    companion object{
        fun rotateImage(source:Bitmap,angle:Float ):Bitmap{

            val matrix = Matrix()
            matrix.postRotate(angle)
            return Bitmap.createBitmap(
                source,0,0,source.width,source.height,matrix,true
            )
        }
    }
}