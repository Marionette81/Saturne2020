<%-- 
    Document   : creerAutoEval
    Created on : 12 mars 2020, 18:20:59
    Author     : Marion 
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link href="css/main.css" rel="stylesheet" type="text/css" />
        <script type="text/javascript" src="js/jquery-3.4.1.js"></script>
        <title>JSP Page</title>
    </head>
    <body>
        <header>
            </br>
            <a href="https://www.ec-nantes.fr/version-francaise/">
                <img src="img/LogoCN_Blanc.png" style="height: 75px; position: absolute; top: 10px; left: 10px;" alt="LogoCN">
            </a>
            <form action="index.do" method="GET">
                <input type="hidden" name="code" value="${code}">
                <h2><INPUT border=0 src="img/logo5.png" type=image Value=submit style="height: 100px;">
                </h2>
            </form>
            <nav>
                <ul>
                    <li class="deroulant"><a href="#">${nom} ${prenom} &ensp;</a>
                        <ul class="sous">
                            <li><form action="affListeAutoEval.do" method="POST">
                                <input type="hidden" name="code" value="${code}">
                                <li><input type="submit" class="menu" value="Historique des auto-évaluations"></li>
                            </form></li>
                            <li><a href="#">Paramètres</a></li>
                            <form action="affResultatEtudiant.do" method="POST">
                                <input type="hidden" name="code" value="${code}">
                                <li><input type="submit" class="menu" value="Voir ses résultats"></li>
                            </form>
                            <form action="disconnect.do" method="GET">
                                <input type="hidden" name="code" value="${code}">
                                <li><input type="submit" class="menu" value="Déconnexion"></li>
                            </form>
                        </ul>
                    </li>
                </ul>
            </nav>
            </br>
        </header>
        <div id="centre" class="box">
            <h1 style="text-align: center;">${test.nomQuiz}</h1>
            <form action="corrigerAutoEval.do" method="POST">
                <input type="hidden" value="${nombre}" name="nombre"/>
                <input type="hidden" value="${test.testId}" name="testId"/>
                <input type="hidden" value="${code}" name="code"/>
                <c:forEach var="question" items="${quesRep}">
                    <c:choose>
                        <c:when test= "${question.ordre%2==0}">
                            <table id="noir">
                        </c:when>
                        <c:otherwise>
                            <table>
                        </c:otherwise>    
                    </c:choose>
                                <tr>
                                    <td>${question.enonceQues}</td>
                                <input name="q${question.ordre}" type="hidden" value="${question.idQues}"/>
                                </tr>
                                 <c:forEach var="reponse" items="${question.listeReponses}">
                                    <tr>
                                        <td>
                                            <c:choose>
                                                <c:when test = "${question.repUni == true}">
                                                    <input name="r${question.idQues}" type="radio" value="${reponse.id}"/>
                                                    <label for="${question.idQues}">${reponse.ennonce}</label>
                                                </c:when>
                                                <c:otherwise>
                                                    <input name="r${question.idQues}" type="checkbox" value="${reponse.id}"/>
                                                    <label for="${question.idQues}">${reponse.ennonce}</label>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:forEach>
                        </table>
                </c:forEach> 
                <button>Corriger</button>
            </form>
                
        </div>
        
    </body>
</html>
