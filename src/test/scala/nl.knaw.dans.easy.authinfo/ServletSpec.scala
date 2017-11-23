/**
 * Copyright (C) 2017 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.authinfo

import java.util.UUID

import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.http.HttpStatus._
import org.scalamock.scalatest.MockFactory
import org.scalatra.test.scalatest.ScalatraSuite

class ServletSpec extends TestSupportFixture
  with ScalatraSuite
  with MockFactory {

  private class Wiring extends ApplicationWiring(new Configuration("", new PropertiesConfiguration() {
    addProperty("bag-store.url", "http://localhost:20110/")
  }))
  private val wiring = mock[Wiring]
  private val app = new EasyAuthInfoApp(wiring)
  addServlet(new EasyAuthInfoServlet(app), "/*")

  private val uuid = UUID.randomUUID()

  "get /" should "return the message that the service is running" ignore {
    get("/") {
      body shouldBe "EASY File Index is running."
      status shouldBe SC_OK
    }
  }
}