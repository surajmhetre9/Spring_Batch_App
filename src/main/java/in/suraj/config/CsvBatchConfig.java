package in.suraj.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import in.suraj.entity.Customer;
import in.suraj.repo.CustomerRepository;

@Configuration
@EnableBatchProcessing
public class CsvBatchConfig {

    @Autowired
    private CustomerRepository customerRepo;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    // Reader
    @Bean
    public FlatFileItemReader<Customer> customerReader() {

        FlatFileItemReader<Customer> itemReader = new FlatFileItemReader<>();

        itemReader.setResource(new FileSystemResource("src/main/resources/customers.csv"));
        itemReader.setName("csv-reader");
        itemReader.setLinesToSkip(1);
        itemReader.setLineMapper(lineMapper());

        return itemReader;
    }

    private LineMapper<Customer> lineMapper() {

        DefaultLineMapper<Customer> lineMapper = new DefaultLineMapper<>();

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(",");
        tokenizer.setStrict(false);

        tokenizer.setNames("id", "firstName", "lastName", "email", "gender", "contactNo", "country", "dob");

        BeanWrapperFieldSetMapper<Customer> mapper = new BeanWrapperFieldSetMapper<>();
        mapper.setTargetType(Customer.class);

        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(mapper);

        return lineMapper;
    }

    // create a Processor
    @Bean
    public CustomerProcessor customerProcessor() {
        return new CustomerProcessor();
    }

    // Writer 
    @Bean
    public RepositoryItemWriter<Customer> customerWriter() {

        RepositoryItemWriter<Customer> writer = new RepositoryItemWriter<>();
        writer.setRepository(customerRepo);
        writer.setMethodName("save");

        return writer;
    }

    // create Step
    @Bean
    public Step step1() {

        return new StepBuilder("step-1", jobRepository)
                .<Customer, Customer>chunk(10, transactionManager)
                .reader(customerReader())
                .processor(customerProcessor())
                .writer(customerWriter())
                //.taskExecutor(taskExecutor())  // this method is multitasking working  
                .build();
    }

    private TaskExecutor taskExecutor() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		executor.setConcurrencyLimit(10); // 10 Thread processes at a time 
		return executor;
	}

	// create Job
    @Bean
    public Job job() {

        return new JobBuilder("customers-job", jobRepository)
                .start(step1())
                .build();
    }

}