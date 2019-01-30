package com.moneymoney.app.accountsservice.receiver;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.moneymoney.app.accountsservice.resource.AccountResource;
import com.moneymoney.app.transactionsservice.entity.Transaction;

@Component

public class AccountReceiver {

	@Autowired
	AccountResource resource;
	
	@Bean
	public Queue queue() {
		return new Queue("AccountQ", false);

	}
	
	@RabbitListener(queues = "AccountQ")
	public void updateBalanceQ( Transaction  transaction) {
		System.out.println("Recieved"+transaction.getCurrentBalance());
		resource.updateAccountBalance(transaction);
	}

}
