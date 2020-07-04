package com.bagi.soreknetmanager.managers

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

object FirebaseManager {
    private val databaseReference = FirebaseDatabase.getInstance().reference
    private val newsRef = databaseReference.child("newsAndAds")

    fun writeAdvertisement(urlAdvertising: String) {
        databaseReference.child("advertising").setValue(urlAdvertising)
    }

    @ExperimentalCoroutinesApi
    suspend fun getLastNumber() = callbackFlow<String> {
        val query = newsRef.orderByKey().limitToFirst(1)
        val listener = object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) { close() }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (newsItem: DataSnapshot in dataSnapshot.children) {
                    newsItem.key?.let { offer(it) }
                }
            }
        }
        query.addListenerForSingleValueEvent(listener)
        awaitClose { databaseReference.removeEventListener(listener) }
    }

    fun uploadNewsToFirebase(link: String, idNews: Int) {
        newsRef.child(idNews.toString()).child("img").setValue(link)
    }

    fun uploadLongNewsToFirebase(
        title: String?,
        date: String?,
        content: String?,
        link: String,
        idNews: Int
    ) {
        if (!title.isNullOrBlank() &&
            !date.isNullOrBlank() &&
            !content.isNullOrBlank() &&
            !link.isBlank())
        newsRef.child(idNews.toString()).child("title").setValue(title)
        newsRef.child(idNews.toString()).child("content").setValue(content)
        newsRef.child(idNews.toString()).child("date").setValue(date)
        newsRef.child(idNews.toString()).child("img").setValue(link)
    }
}