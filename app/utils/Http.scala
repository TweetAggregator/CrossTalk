package utils

import org.apache.http._
import org.apache.http.entity.StringEntity
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpGet
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.BufferedOutputStream
import java.io.InputStream

object Http{

	def askFor(request: String) = {
	    val httpRequest = new HttpGet(request)
	    httpRequest.addHeader("Content-Type", "application/x-www-form-urlencoded")
	    val client = new DefaultHttpClient()
	    // println(httpRequest)
	    val twitterResponse = client.execute(httpRequest) /* Send the request and get the response */
	    twitterResponse.getEntity().getContent()
	}

	/** Read all the data present in the InputStream in and return it as a String */
	def readAll(in: InputStream): String = {
		val inr = new BufferedReader(new InputStreamReader(in))
		val bf = new StringBuilder
		var rd = inr.readLine
		while (rd != null) { bf.append(rd); rd = inr.readLine }
		bf.toString
	}	
}

