package com.moneymoney.web.controller;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import com.moneymoney.web.entity.CurrentDataSet;
import com.moneymoney.web.entity.Transaction;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

@EnableCircuitBreaker
@Controller
public class BankAppController {

	private CurrentDataSet storeDataSet;
	@Autowired
	private RestTemplate restTemplate;

	@RequestMapping("/deposit")
	public String depositForm() {
		return "DepositForm";
	}

	@HystrixCommand(fallbackMethod = "noService")
	@RequestMapping(value = "/deposit", method = RequestMethod.POST)
	public String deposit(@ModelAttribute Transaction transaction, Model model) {

		restTemplate.postForEntity("http://api-client/transactions-client/transactions", transaction, null);

		model.addAttribute("message", "Success!");
		return "DepositForm";
	}

	public String noService(@ModelAttribute Transaction transaction, Model model) {
		String message = "Service Not Available";
		model.addAttribute("message", message);
		return "NoService";

	}

	@RequestMapping("/withdraw")
	public String withdrawForm() {
		return "WithdrawForm";

	}

	@HystrixCommand(fallbackMethod = "noService")
	@RequestMapping(value = "/withdraw", method = RequestMethod.POST)
	public String withdraw(@ModelAttribute Transaction transaction, Model model) {

		restTemplate.postForEntity("http://api-client/transactions-client/transactions/withdraw", transaction, null);
		model.addAttribute("message", "Success!");
		return "WithdrawForm";
	}

	@RequestMapping("/transfer")
	public String fundTransferForm() {
		return "FundTransfer";
	}

	@HystrixCommand(fallbackMethod = "noFundService")
	@RequestMapping(value = "/transfer", method = RequestMethod.POST)
	public String fund(@RequestParam("senderAccountNumber") Integer senderAccountNumber,
			@RequestParam("amount") Double amount, @RequestParam("receiverAccountNumber") Integer receiverAccountnumber,
			Model model) {
		Transaction transaction = new Transaction();
		transaction.setAccountNumber(senderAccountNumber);
		transaction.setAmount(amount);
		transaction.setTransactionDetails("Online");
		restTemplate.postForEntity("http://api-client/transactions-client/transactions/transfer?receiverAccountnumber="
				+ receiverAccountnumber, transaction, null);
		model.addAttribute("message", "Success!!!");
		return "FundTransfer";
	}

	public String noFundService(@RequestParam("senderAccountNumber") Integer senderAccountNumber,
			@RequestParam("amount") Double amount, @RequestParam("receiverAccountNumber") Integer receiverAccountnumber,
			Model model) {
		String funMessage = "Service Not Available,transaction failed try later";
		model.addAttribute("message", funMessage);
		return "NoService";

	}

	@HystrixCommand(fallbackMethod = "noStatements")
	@RequestMapping("/statement")
	public ModelAndView getStatementDeposit(@RequestParam("offset") int offset, @RequestParam("size") int size) {
		CurrentDataSet currentDataSet = restTemplate
				.getForObject("http://api-client/transactions-client/transactions/statement", CurrentDataSet.class);
		storeDataSet = currentDataSet;
		int currentSize = size == 0 ? 5 : size;
		int currentOffset = offset == 0 ? 1 : offset;
		Link next = linkTo(
				methodOn(BankAppController.class).getStatementDeposit(currentOffset + currentSize, currentSize))
						.withRel("next");
		Link previous = linkTo(
				methodOn(BankAppController.class).getStatementDeposit(currentOffset - currentOffset, currentSize))
						.withRel("previous");
		List<Transaction> currentDataSetList = new ArrayList<Transaction>();
		List<Transaction> transactions = currentDataSet.getTransactions();
		System.out.println(transactions);

		for (int i = currentOffset - 1; i < currentSize + currentOffset - 1; i++) {
			if ((transactions.size() <= i && i > 0) || currentOffset < 1)
				break;
			Transaction transaction = transactions.get(i);
			currentDataSetList.add(transaction);
		}

		CurrentDataSet dataSet = new CurrentDataSet(currentDataSetList, next, previous);
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addObject("currentDataSet", dataSet);
		modelAndView.setViewName("statements");
		return modelAndView;
	}

	public ModelAndView noStatements(@RequestParam("offset") int offset, @RequestParam("size") int size) {
		CurrentDataSet currentDataSet = storeDataSet;
		int currentSize = size == 0 ? 5 : size;
		int currentOffset = offset == 0 ? 1 : offset;
		Link next = linkTo(
				methodOn(BankAppController.class).getStatementDeposit(currentOffset + currentSize, currentSize))
						.withRel("next");
		Link previous = linkTo(
				methodOn(BankAppController.class).getStatementDeposit(currentOffset - currentOffset, currentSize))
						.withRel("previous");
		List<Transaction> currentDataSetList = new ArrayList<Transaction>();
		List<Transaction> transactions = currentDataSet.getTransactions();
		System.out.println(transactions);

		for (int i = currentOffset - 1; i < currentSize + currentOffset - 1; i++) {
			if ((transactions.size() <= i && i > 0) || currentOffset < 1)
				break;
			Transaction transaction = transactions.get(i);
			currentDataSetList.add(transaction);
		}

		CurrentDataSet dataSet = new CurrentDataSet(currentDataSetList, next, previous);
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addObject("currentDataSet", dataSet);
		modelAndView.setViewName("statements");
		modelAndView.addObject("message","Service Down You won't be able to view Your updated statements.Try Again later ");
		return modelAndView;

	}

}
