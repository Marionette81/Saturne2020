/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.centrale.pgrou.controllers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.centrale.ldap.LDAPManager;
import org.centrale.ldap.LDAPUser;
import org.centrale.pgrou.items.*;
import org.centrale.pgrou.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author louis-alexandre
 */
@Controller
public class StartupController {
    
    @Autowired
    private ConnexionRepository connexionRepository;    
    @Autowired
    private PersonneRepository personneRepository;    
    @Autowired
    private RoleRepository roleRepository;    
    @Autowired
    private MenuRepository menuRepository;    
    @Autowired
    private TestRepository testRepository;    
    @Autowired
    private GroupeRepository groupeRepository;    
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private NotationRepository notationRepository;
    @Autowired
    private QuizRepository quizRepository;
    
    @RequestMapping(value="index.do", method=RequestMethod.GET)
    public ModelAndView handleGet(HttpServletRequest request){
        ModelAndView returned = new ModelAndView("index");
        String code = request.getParameter("code");
        if(code!=null){
            Optional<Connexion> coResult = connexionRepository.findById(code);
            if(coResult.isPresent()){
                Connexion connexion = coResult.get();
                Personne pers = connexion.getPersonneid();
                Role admin = roleRepository.findOneById(1);
                Role teacher = roleRepository.findOneById(2);
                if(pers.getRoleCollection().contains(admin)||pers.getRoleCollection().contains(teacher)){
                    returned = new ModelAndView("AccueilProfesseur");
                    int persId = pers.getPersonneid();
                    returned.addObject("listTests",testRepository.affichagePrecedentsTests(persId));
                } else {
                    returned = new ModelAndView("AccueilEtudiant");
                    String login = pers.getLogin();
                    returned.addObject("listTests",testRepository.affichageProchainsTests(login));
                }
                Security.setDefaultData(returned, connexion);
            }
        } else {
            ApplicationInitializer.init(personneRepository, roleRepository, menuRepository);
            Security.check(connexionRepository);
        }
        Security.check(connexionRepository);
        return returned;
    }
    
    @RequestMapping(value="index.do", method=RequestMethod.POST)
    public ModelAndView handlePost(HttpServletRequest request){
        ModelAndView returned = new ModelAndView("index");
        Security.check(connexionRepository);
        
        String login = request.getParameter("user");
        String mdp = request.getParameter("password");
        
        Boolean identifiantsCorrects = false;
        
        if ((!login.isEmpty()) && (!mdp.isEmpty())){
            Personne person = personneRepository.findByLogin(login);
            
            //Règle 1 : utilisation du LDAP
            if((LDAPManager.identifyLDAP(login, mdp))) {
                identifiantsCorrects = true;
                if (person == null){
                    LDAPUser ldapUser = LDAPManager.searchLDAP(login, mdp);
                    person = new Personne();
                    person.setNom(ldapUser.getUserNom());
                    person.setPrenom(ldapUser.getUserPrenom());
                    person.setLogin(login);
                    if (ldapUser.getUserAffiliation().equals("student")){
                        Role role = roleRepository.findOneByLabel("Student");
                        Collection<Role> roleCollection = new ArrayList();
                        roleCollection.add(role);
                        person.setRoleCollection(roleCollection);
                        Collection<Personne> persCollection = new ArrayList();
                        persCollection.add(person);
                        role.setPersonneCollection(persCollection);
                    }
                    personneRepository.save(person);
                }
            }
            //Règle 2 : mot de passe dans la base de données
            else if((person != null)
                && (person.getMotdepasse()!= null) && (!person.getMotdepasse().isEmpty())
                && (Security.validatePassword(mdp, person.getMotdepasse()))) {
                identifiantsCorrects = true;
            }
            //Règle 3 :l'administrateur doit être dans la base de données
            else if((person == null)
                && (login.equals(ApplicationInitializer.TRAPLOGIN)) 
                && (mdp.equals(ApplicationInitializer.TRAPPASS))) {
                ApplicationInitializer.createDefaultLogin(personneRepository, roleRepository);
                person = personneRepository.findByLogin(login);
                identifiantsCorrects = true;
            }
            //Si les identifiants sont corrects, on connecte l'utilisateur
            if(identifiantsCorrects){
                Connexion connexion = connexionRepository.create(request, person);
                
                Role admin = roleRepository.findOneById(1);
                Role teacher = roleRepository.findOneById(2);
                if (person != null && person.getRoleCollection() != null){
                    if(person.getRoleCollection().contains(admin) || person.getRoleCollection().contains(teacher)){
                        returned = new ModelAndView("AccueilProfesseur");
                        int persId = person.getPersonneid();
                        returned.addObject("listTests",testRepository.affichagePrecedentsTests(persId));
                    } else {
                        returned = new ModelAndView("AccueilEtudiant");
                        returned.addObject("listTests",testRepository.affichageProchainsTests(login));
                    }
                }
                Security.setDefaultData(returned, connexion);
            }
        }
        
        return returned;
    }


    
    /**
     * Fonction qui redirige vers la page des résultats 
     * @param request: on récupère l'id de la personne pour avoir la liste des tests qu'il a fait
     * @return 
     */
    @RequestMapping(value="versAffRes.do", method=RequestMethod.POST)
    public ModelAndView envoyerVersAffTest(HttpServletRequest request){
        Security.check(connexionRepository);
        ModelAndView returned = new ModelAndView("affResultat");
        String code = request.getParameter("code");
        if(code!=null){
            Optional<Connexion> coResult = connexionRepository.findById(code);
            if(coResult.isPresent()){
                Connexion connexion = coResult.get();
                Personne pers = connexion.getPersonneid();
                int id = pers.getPersonneid();
                List<Groupe> listGroupe = groupeRepository.findAll();
                List<Test> listTest = testRepository.findWithPersonne(id); 
                Security.setDefaultData(returned, connexion);
                returned.addObject("listGroupe",listGroupe);
                returned.addObject("listTest",listTest);
                returned.addObject("personneId",id);
            }
        }    
        return returned;
    }
    
    /**
     * Fonction qui envoie vers la liste des questions déjà créées
     * @param request: on récupère l'id de la personne pour pouvoir lui associer ses questions
     * @return 
     */
    @RequestMapping(value="versCreerQuest.do", method=RequestMethod.POST)
    public ModelAndView envoyerVersCreerQuest(HttpServletRequest request){
        
        ModelAndView returned = new ModelAndView("question");
        String code = request.getParameter("code");
        Optional<Connexion> coResult = connexionRepository.findById(code);
        if(coResult.isPresent()){
            Connexion connexion = coResult.get();
            Security.setDefaultData(returned, connexion);
            Integer persId = connexion.getPersonneid().getPersonneid();
            Collection<Question> listQuestion = questionRepository.findWithPersonne(persId);
            returned.addObject("listQuestion", listQuestion);
        }
        return returned;
    }
    
    /**
     * Fonction qui envoie vers la page de création de test
     * @param request: id de la personne
     * @return 
     */
    @RequestMapping(value="versCreerTest.do",method=RequestMethod.GET)
    public ModelAndView envoyerVersCreerTest(HttpServletRequest request) {
        ModelAndView returned = new ModelAndView("session");
        String code = request.getParameter("code");
        Optional<Connexion> coResult = connexionRepository.findById(code);
        if(coResult.isPresent()){
            Connexion connexion = coResult.get();
            Security.setDefaultData(returned, connexion);
            Personne pers = connexion.getPersonneid();
            List<Notation> listNotation = notationRepository.findAll();
            List<Groupe> listGroupe = groupeRepository.findAll();
            List<Quiz> listQuiz = quizRepository.findWithPersonne(pers.getPersonneid());
            List<Test> listTest = testRepository.findWithPersonne(pers.getPersonneid());
            returned.addObject("listNotations", listNotation);
            returned.addObject("listGroupes", listGroupe);
            returned.addObject("listQuizs",listQuiz);
            returned.addObject("listTests",listTest);
        }
        return returned;
    }
    
    /**
     * Fonction qui renvoie sur un faux menu de prof où toutes les fonctions sont disponibles
     * @param request: pour l'id de la personne
     * @return 
     */
    @RequestMapping(value="versMenuProf.do", method=RequestMethod.POST)
    public ModelAndView envoyerVersMenuProf(HttpServletRequest request){
        Security.check(connexionRepository);
        String idStr = request.getParameter("id");
        int id = Integer.parseInt(idStr);
        Personne person = personneRepository.findById(id).get();
        String nom = person.getNom();
        String prenom = person.getPrenom();
        ModelAndView returned = new ModelAndView("menuProf");
        returned.addObject("prenom", prenom);
        returned.addObject("nom", nom);
        returned.addObject("personneId", id);
        return returned;
    }
    
    /**
     * Fonction qui envoie vers la liste des tests disponibles
     * @param request: id de la personne
     * @return 
     */
    @RequestMapping(value="versListTest.do", method=RequestMethod.POST)
    public ModelAndView envoyerListTest(HttpServletRequest request){
        Security.check(connexionRepository);
        String idStr = request.getParameter("id");
        int id = Integer.parseInt(idStr);
        ModelAndView returned = new ModelAndView();
        List<Test> listTest = testRepository.findWithParameters(new java.util.Date(),id); //Mettre id <!>
        List<TestAff> listTestAff = new ArrayList();
        String format = "HH:mm";
        java.text.SimpleDateFormat formater = new java.text.SimpleDateFormat( format );
        for (Test aTest: listTest){
            TestAff aTestAff = new TestAff(aTest.getTestid(),aTest.getQuizid().getNomquiz());
            aTestAff.setDureeStr(formater.format(aTest.getDureemaxtest()));
            int duree = aTestAff.stringToInt(formater.format(aTest.getDureemaxtest()));
            aTestAff.setDureeInt(duree);
            listTestAff.add(aTestAff);
        }
        returned = new ModelAndView("affTest");
        returned.addObject("listTests",listTestAff);
        returned.addObject("personneId",id);
        return returned;
    }
    
    /**
     * Fonction qui renvoie sur un faux menu d'élève où toutes les fonctions sont disponibles
     * @param request: pour l'id de la personne
     * @return 
     */
    @RequestMapping(value="versMenuEleve.do", method=RequestMethod.POST)
    public ModelAndView envoyerVersMenuEleve(HttpServletRequest request){
        Security.check(connexionRepository);
//        String idStr = request.getParameter("id");
//        int id = Integer.parseInt(idStr);
//        Personne person = personneRepository.findById(id).get();
//        String nom = person.getNom();
//        String prenom = person.getPrenom();
        ModelAndView returned = new ModelAndView("AccueilEtudiant");
//        returned.addObject("prenom", prenom);
//        returned.addObject("nom", nom);
//        returned.addObject("personneId", id);
        return returned;
    }
}
