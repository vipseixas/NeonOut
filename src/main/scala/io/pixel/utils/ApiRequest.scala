package io.pixel.utils

import sttp.client3.*
import sttp.model.*
import scala.util.{Success,Failure}
import scala.util.control.Exception.allCatch
import scala.annotation.tailrec

object ApiRequest {
	private val maxSequentialRequests = 10
	private val requestSleep = 2000
	private var requestCounter: Int = maxSequentialRequests

	private val maxRetries: Int = 5
	private val retryInterval: Int = 10

	private val backend: ThreadLocal[SttpBackend[Identity, Any]] = ThreadLocal.withInitial(() => HttpClientSyncBackend())

	@tailrec
	def sendRequest(uri: Uri, retry: Int = 0): Either[String, String] =
		checkRequestWait()

		allCatch.withTry {
			basicRequest.get(uri).send(backend.get)
		} match
			case Success(response) =>
				if response.code.isSuccess then
					response.body
				else
					Left(s"${response.code}: ${response.statusText}")
			case Failure(e) =>
				print(s"\n## Error fetching API: ${e.getMessage}. ")
				if retry < maxRetries then
					println(s"Retrying in $retryInterval seconds...")
					Thread.sleep(retryInterval * 1000)
					sendRequest(uri, retry + 1)
				else
					println(s"Giving up after $retry retries.")
					Left(e.getMessage)


	def downloadBinary(uri: Uri): Identity[Response[Array[Byte]]] =
		// The assets come from cloudfront and so we will not restrict throughput
		basicRequest
			.get(uri)
			.response(asByteArrayAlways)
			.send(backend.get)


	private def checkRequestWait(): Unit =
		requestCounter -= 1

		if requestCounter <= 0 then
			Thread.sleep(requestSleep)
			requestCounter = maxSequentialRequests

}
