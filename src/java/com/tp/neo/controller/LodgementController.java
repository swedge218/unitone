/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tp.neo.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tp.neo.exception.SystemLogger;
import com.tp.neo.model.Customer;
import com.tp.neo.controller.components.AppController;
import com.tp.neo.controller.helpers.CompanyAccountHelper;
import com.tp.neo.controller.helpers.MorgageList;
import com.tp.neo.interfaces.SystemUser;
import com.tp.neo.model.CustomerAgent;
import com.tp.neo.model.Lodgement;
import com.tp.neo.model.LodgementItem;
import com.tp.neo.model.ProductOrder;
import com.tp.neo.model.OrderItem;
import com.tp.neo.model.ProductOrder;
import com.tp.neo.model.utils.TrailableManager;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.bind.PropertyException;
import com.google.gson.reflect.TypeToken;
import com.tp.neo.controller.helpers.LodgementManager;
import com.tp.neo.model.Agent;
import com.tp.neo.model.CompanyAccount;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.TimeZone;
import java.util.logging.Level;
import javax.persistence.RollbackException;

/**
 *
 * @author John
 */
@WebServlet(name = "LodgementController", urlPatterns = {"/Lodgement"})
public class LodgementController extends AppController {
    private static String INSERT_OR_EDIT = "/user.jsp";
    private static String LODGEMENT_ADMIN = "/views/lodgement/admin.jsp"; 
    private static String LODGEMENT_NEW_AGENT = "/views/lodgement/customer_orders.jsp";
    private static String LODGEMENT_NEW = "/views/lodgement/lodge.jsp";
    private static String LODGEMENT_APPROVAL = "/views/lodgement/approval.jsp";
    private static String LODGEMENT_SUCCESS = "/views/lodgement/success.jsp";
    private final static Logger LOGGER = 
            Logger.getLogger(Lodgement.class.getCanonicalName());
    
     private HashMap<String, String> errorMessages = new HashMap<String, String>();
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet LodgmentController</title>");            
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet LodgmentController at " + request.getContextPath() + "</h1>");
            out.println("</body>");
            out.println("</html>");
        } finally {
            out.close();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        //processRequest(request, response);
        processGetRequest(request, response);
    }

      protected void processGetRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
    
        SystemUser user = sessionUser;
        
        String action = request.getParameter("action") != null?request.getParameter("action"):"";
        
        long userTypeId = user.getSystemUserTypeId();
        
         
         String viewFile = LODGEMENT_ADMIN;
         
         if(action.equals("new")){
             
             viewFile = LODGEMENT_NEW;
             
             request.setAttribute("companyAccount", CompanyAccountHelper.getCompanyAccounts());
             
             if(userTypeId == 1)
                 request.setAttribute("customers", listCustomers());
             else if(userTypeId == 2)
                 request.setAttribute("customers", listAgentCustomers(user.getSystemUserId()));
             
            
            
         }
         else if(action.equals("getOrders")) {
             
             listCustomerOrders(request, response);
         }
         else if(action.equalsIgnoreCase("approval")){
             
             viewFile = LODGEMENT_APPROVAL;
             getUnapprovedLodgement(request);
             
         }
         else if(action.equals("success")){
             viewFile = LODGEMENT_SUCCESS;
         }
         else{
            viewFile = LODGEMENT_ADMIN;
            request.setAttribute("lodgements",listLodgements());;
         }
        
            RequestDispatcher dispatcher = request.getRequestDispatcher(viewFile);
            dispatcher.forward(request, response);
            
    }
    
    
    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String action = request.getParameter("action");
        
        if(action.equalsIgnoreCase("morgage")){
             
             String viewFile = LODGEMENT_NEW;
             payMorgage(request);
             String contextPath = request.getContextPath();
             response.sendRedirect(contextPath + "/Lodgement?action=success");
         }
        else{
            processInsertRequest(request, response);
        }
    }

    
    protected void processInsertRequest(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException{
             
            
    }
    
    
    
    /*TP: Validation is done here*/
    private void validate(HttpServletRequest request, HttpServletResponse response) throws PropertyException, ServletException, IOException{
         
         errorMessages.clear();
         if(request.getParameter("productAmountToPay").isEmpty()){
         errorMessages.put("1","Amount to pay field is required");
         
         }
        if(request.getParameter("paymentMethod").isEmpty()){
         errorMessages.put("2","Select Payment Method");
         }
         else if(!request.getParameter("paymentMethod").isEmpty()){
         if(Integer.parseInt(request.getParameter("paymentMethod"))==1){
          if(request.getParameter("bankName").isEmpty()){
         errorMessages.put("3","For the payment method selected, Bank Name is required");
         }
          if(request.getParameter("depositorsName").isEmpty()){
         errorMessages.put("4","For the payment method selected, Depositor's Name is required");
         }
          
          if(request.getParameter("tellerNumber").isEmpty()){
         errorMessages.put("5","For the payment method selected, Teller Number is required");
         }
          
          if(request.getParameter("tellerAmount").isEmpty()){
         errorMessages.put("6","For the payment method selected, Amount paid is required");
         }
               
         }
         else if(Integer.parseInt(request.getParameter("paymentMethod"))==3){
         if(request.getParameter("cashAmount").isEmpty()){
            errorMessages.put("7","For the payment method selected, Amount paid is required");
         }
             
         }
         }
     }
    
    /*TP: Listing of customers that exists in the database and are not deleted*/
    public List<Lodgement> listLodgements(){
        
        
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("NeoForcePU");
        EntityManager em = emf.createEntityManager();
        int i = 1;
        Long id = new Long(i);
        //find by ID
        Query jpqlQuery  = em.createNamedQuery("Lodgement.findAll");
       
        List<Lodgement> lodgementList = jpqlQuery.getResultList();
        
        System.out.println("This is the new lodgement details "+lodgementList);
        return lodgementList;
    }
    
    public List<Customer> listCustomers(){
        
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("NeoForcePU");
        EntityManager em = emf.createEntityManager();
        
        Query jplQuery = em.createNamedQuery("Customer.findAll");
        
        List<Customer> custResultList = jplQuery.getResultList();
        
        return custResultList;
    }
    
    public List<Customer> listAgentCustomers(long agentId) {
        
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("NeoForcePU");
        EntityManager em = emf.createEntityManager();
        
        Query jplQuery = em.createNamedQuery("CustomerAgent.findByAgentId");
        jplQuery.setParameter("agentId", agentId);
        
        List<CustomerAgent> customerAgentResultList = jplQuery.getResultList();
        
        List<Customer> customerList = new ArrayList<Customer>();
        
        for(CustomerAgent custAgent : customerAgentResultList){
            
            customerList.add(custAgent.getCustomer());
        }
        
        return customerList;
        
    }
    
    public void listCustomerOrders(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
    
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("NeoForcePU");
        EntityManager em = emf.createEntityManager();
        
        Gson gson = new GsonBuilder().create();
        
        long customerId = Long.parseLong(request.getParameter("customerId"));
        
        Customer customer = em.find(Customer.class, customerId);
        
        Query jplQuery = em.createNamedQuery("ProductOrder.findByCustomer");
        
        jplQuery.setParameter("customerId", customer);
        
        System.out.println("Query : " + jplQuery.toString());
        List<ProductOrder> orderResultSet = jplQuery.getResultList();
        
        List<Map> mapList = new ArrayList<Map>();
        
        for(ProductOrder order : orderResultSet)
        {
            Map<String, String> map = new HashMap<String, String>();
            
            map.put("id", order.getId().toString());
            map.put("customerName", order.getCustomer().getLastname() + " " + order.getCustomer().getFirstname());
            map.put("agentName", order.getAgent().getLastname() + " " + order.getAgent().getFirstname());
            map.put("sales",gson.toJson(getSalesByOrder(order)));
            
            mapList.add(map);
        }
        
        String jsonResponse = gson.toJson(mapList);
        
       
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonResponse);
        response.getWriter().flush(); 
        response.getWriter().close();
        System.out.println("jsonResponse: " + jsonResponse);
       
        
    }
    
    private List<Map> getSalesByOrder(ProductOrder order) {
        List<Map>OrderItemsList = new ArrayList();
        
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("NeoForcePU");
        EntityManager em = emf.createEntityManager();
        
        
        Query jplQuery = em.createNamedQuery("OrderItem.findByOrder");
        jplQuery.setParameter("order", order);
        
        List<OrderItem> resultSet = jplQuery.getResultList();
        
        for(OrderItem orderItem : resultSet) {
            
            short status = orderItem.getApprovalStatus();
            
            Map orderItemDetail = isOrderItemPaymentCompleted(orderItem);
            //If order is declined skip
            if(status == 2){
                continue;    
            }
            
            if((boolean)orderItemDetail.get("isComplete")){
                continue;
            }
            
            //Check if the OrderItem payment has been completed
            
            
            Map<String, String> map = new HashMap();
            Double remainingAmt = (orderItem.getUnit().getAmountPayable() * orderItem.getQuantity()) - ((Double)orderItemDetail.get("totalPaid"));
            map.put("saleId",orderItem.getId().toString());
            map.put("project", orderItem.getUnit().getProject().getName());
            map.put("unitName", orderItem.getUnit().getTitle());
            map.put("unitQty",orderItem.getQuantity().toString());
            map.put("initialDeposit",orderItem.getInitialDep().toString());
            map.put("amountPayable", remainingAmt.toString());
            map.put("amountPaid", ((Double)orderItemDetail.get("totalPaid")).toString());
            map.put("monthlyPay", ((Double)orderItem.getUnit().getMonthlyPay()).toString());
            
            OrderItemsList.add(map);
        }
        
        
        emf.getCache().evictAll();
        em.close();
        emf.close();
        
        return OrderItemsList;
    }
    
    private Map isOrderItemPaymentCompleted(OrderItem orderItem){
        
        Map<String, Object> map = new HashMap();
        
        List<LodgementItem> lodgementItemList = (List)orderItem.getLodgementItemCollection();
        
        boolean isComplete = false;
        
        double totalPaid = 0;
        
        for(LodgementItem lodgementItem : lodgementItemList){
            
            totalPaid += lodgementItem.getAmount();
        }
        
        double unitAmount = orderItem.getUnit().getAmountPayable();
        
        if(unitAmount <= totalPaid){
            isComplete = true;
        }
        
        map.put("isComplete", isComplete);
        map.put("totalPaid", totalPaid);
        
        return map;
    }
    
    private void getUnapprovedLodgement(HttpServletRequest request){
        
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("NeoForcePU");
        EntityManager em = emf.createEntityManager();
        
        Query jplQuery = em.createNamedQuery("Lodgement.findByApprovalStatus");
        jplQuery.setParameter("approvalStatus", (short)0);
        List<Lodgement> lodgementList = jplQuery.getResultList();
        
        request.setAttribute("notificationLodgementId",0);
        request.setAttribute("lodgements", lodgementList);
    }
    
    private void payMorgage(HttpServletRequest request) {
        
        try {
            EntityManagerFactory emf = Persistence.createEntityManagerFactory("NeoForcePU");
            EntityManager em = emf.createEntityManager();
            
            String morgageItemJson = request.getParameter("orderItemsJson");
            List<MorgageList> morgageList = processJson(morgageItemJson);
            
            SystemUser user = sessionUser;
            String type = userType;
            
            Customer customer = null;
            
            Long userId = user.getSystemUserId();
            Lodgement lodgement = prepareLodgement(getRequestParameters(request), userId);
            
            List<LodgementItem> lodgementItemList = new ArrayList();
            
            for(MorgageList morgageItem : morgageList){
                
                OrderItem orderItem = em.find(OrderItem.class, (long) morgageItem.getOrderItemId());
                LodgementItem lodgementItem = new LodgementItem();
                
                lodgementItem.setCreatedDate(getDateTime().getTime());
                lodgementItem.setCreatedBy(userId);
                lodgementItem.setItem(orderItem);
                lodgementItem.setAmount(morgageItem.getAmount());
                
                lodgementItemList.add(lodgementItem);
                
                if(customer == null){
                    customer = orderItem.getOrder().getCustomer();
                }
                
            }
            
            LodgementManager lodgementManager = new LodgementManager(user);
            lodgement = lodgementManager.processLodgement(customer, lodgement, lodgementItemList, request.getContextPath());
            
            Map map = prepareMorgageInvoice(lodgement, lodgementItemList);
            
            HttpSession session = request.getSession(false);
            session.setAttribute("invoice",map);
            
            em.close();
            emf.close();
            
        } catch (PropertyException ex) {
            Logger.getLogger(LodgementController.class.getName()).log(Level.SEVERE, null, ex);
            
        } catch (RollbackException ex) {
            Logger.getLogger(LodgementController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    private Map prepareMorgageInvoice(Lodgement lodgement, List<LodgementItem> lodgementItems){
        Map<String,Object> map = new HashMap();
        
        List<Map> itemList = new ArrayList();
        
        for(LodgementItem item : lodgementItems){
            
            Map<String, String> itemMap = new HashMap();
            
            String title = item.getItem().getUnit().getTitle();
            Double amount = item.getAmount();
            
            itemMap.put("title",title);
            itemMap.put("amount", amount.toString());
            
            itemList.add(itemMap);
        }
       
        System.out.println("Lenght of items : " + itemList.size());
        map.put("total",lodgement.getAmount());
        map.put("items",itemList);
        
        return map;
    }
    
    private List<MorgageList> processJson(String jsonString){
        
        Gson gson = new GsonBuilder().create();
        Type listType = new TypeToken<ArrayList<MorgageList>>(){}.getType();
        List<MorgageList> morgageItems = gson.fromJson(jsonString, listType);
        
        return morgageItems;
    }
    
    public Map getRequestParameters(HttpServletRequest request) {
        
        Map<String, String> map = new HashMap();
        Enumeration params = request.getParameterNames();
        while(params.hasMoreElements())
        {
            String elem = params.nextElement().toString();
            map.put(elem, request.getParameter(elem));
        }
        
        return map;
    }
    
    private Lodgement prepareLodgement(Map request, Long userId) {
        
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("NeoForcePU");
        EntityManager em = emf.createEntityManager();
        
        Lodgement lodgement = new Lodgement();
        
        Short paymentMethod = Short.parseShort(request.get("paymentMethod").toString());
        
        CompanyAccount companyAccount = em.find(CompanyAccount.class, Integer.parseInt(request.get("companyAccount").toString()));
        
        lodgement.setPaymentMode(paymentMethod);
        lodgement.setCreatedDate(this.getDateTime().getTime());
        lodgement.setCreatedBy(userId);
        lodgement.setCompanyAccountId(companyAccount);
        lodgement.setApprovalStatus((short)0);
        
        if(paymentMethod == 1) {
            lodgement.setTransactionId(request.get("tellerNumber").toString());
            lodgement.setAmount(Double.parseDouble(request.get("tellerAmount").toString()));
            lodgement.setDepositorName(request.get("depositorsName").toString());
            lodgement.setLodgmentDate(getDateTime().getTime());
        }
        else if(paymentMethod == 3){
            
            lodgement.setAmount(Double.parseDouble(request.get("cashAmount").toString()));
            lodgement.setLodgmentDate(getDateTime().getTime());
        }
        else if(paymentMethod == 4) {
            
            lodgement.setAmount(Double.parseDouble(request.get("transfer_amount").toString()));
            lodgement.setOriginAccountName(request.get("transfer_accountName").toString());
            lodgement.setOriginAccountNumber(request.get("transfer_accountNo").toString());
            lodgement.setLodgmentDate(getDateTime().getTime());
            
        }
        
        return lodgement;
    }
    
    private Calendar getDateTime(){
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Africa/Lagos"));
        return calendar;
    }
    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}