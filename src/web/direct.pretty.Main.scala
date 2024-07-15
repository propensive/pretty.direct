package direct.pretty

import soundness.*
import honeycomb.*
import punctuation.*
import scintillate.*

import logFormats.ansiStandard
import classloaders.scala
import charEncoders.utf8
import charDecoders.utf8
import textSanitizers.skip
import orphanDisposal.cancel
import threadModels.platform
import serpentine.hierarchies.simple
import stdioSources.virtualMachine.ansi
import htmlRenderers.scalaSyntax

given Realm = realm"prettydirect"
given Message is Loggable = safely(supervise(Log.route(Out))).or(Log.silent)
given Online = Online

def page(content: Html[Flow]*): HtmlDoc =
  HtmlDoc(Html
   (Head
     (Title(t"Pretty.direct"),
      Meta(charset = enc"UTF-8"),
      Link(rel = Rel.Stylesheet, href = % / p"styles.css")),
    Body
     (Main(content),
      Footer(P(t"© Copyright 2024 Jon Pretty")))))

@main
def server(): Unit =
  quash:
    case ConcurrencyError(reason) =>
      Out.println(m"There was a concurrency error")
      ExitStatus.Fail(2).terminate()
  .within:
    supervise(tcp"8080".serve[Http](handle))

class Service() extends JavaServlet(handle)

def handle(using HttpRequest): HttpResponse[?] =
  quash:
    case MarkdownError(detail) =>
      HttpResponse(page(Aside, H1(t"Bad markdown")))

    case ClasspathError(path) =>
      HttpResponse(page(Aside, H1(t"Path $path not found")))

    case PathError(path, reason) =>
      HttpResponse(page(Aside, P(t"$path is not valid because $reason")))

  .within:
    request.path match
      case % | (% / p"statement.html") =>
        HttpResponse(Redirect(url"https://pretty.direct/statement"))

      case % / p"styles.css" =>
        HttpResponse(Classpath / p"styles.css")

      case % / p"consentorder.pdf" =>
        HttpResponse(Classpath / p"consentorder.pdf")

      case % / p"statement" =>
        val markdown = Markdown.parse((Classpath / p"statement.md").read[Text])
        HttpResponse(page(markdown.html*))

      case _ =>
        HttpResponse(page(Div, H1(t"Not found"), P(t"This page does not exist.")))
