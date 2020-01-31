package kinesisencryption.streams;

import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.*;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import kinesisencryption.utils.KinesisEncryptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.codeguruprofilerjavaagent.Profiler;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EncryptedConsumerWithStreams {
	private static final Logger log = LoggerFactory.getLogger(EncryptedConsumerWithStreams.class);
	// private final static CharsetDecoder decoder =
	// Charset.forName("UTF-8").newDecoder();

	public static void main(String[] args) throws Exception {
		/*AmazonKinesisClient kinesis = new AmazonKinesisClient(
				new DefaultAWSCredentialsProviderChain().getCredentials());*/
		AmazonKinesis kinesis = AmazonKinesisClientBuilder.standard().build();
		/*AWSKMSClient kms = new AWSKMSClient(new DefaultAWSCredentialsProviderChain().getCredentials());*/
		AWSKMS kms = AWSKMSClientBuilder.standard().build();
		//kinesis.setEndpoint(KinesisEncryptionUtils.getProperties().getProperty("kinesis_endpoint"));
		//kms.setEndpoint(KinesisEncryptionUtils.getProperties().getProperty("kms_endpoint"));
		String keyArn = KinesisEncryptionUtils.getProperties().getProperty("key_arn");
		String encryptionContext = KinesisEncryptionUtils.getProperties().getProperty("encryption_context");
		log.info("Successfully retrieved keyarn property " + keyArn);
		log.info("Successfully retrieved encryption context property " + encryptionContext);

		new Profiler.Builder()
				.profilingGroupName("one1")
				.build().start();
		final Map<String, String> context = Collections.singletonMap("Kinesis", encryptionContext);

		try {
			String streamName = KinesisEncryptionUtils.getProperties().getProperty("stream_name");
			log.info("Stream name from properties is " + streamName);
			DescribeStreamResult streamResult = kinesis.describeStream(streamName);
			StreamDescription streamDescription = streamResult.getStreamDescription();
			List<Shard> shardList = streamDescription.getShards();
			log.info("Shard size is " + shardList.size());

			for (Shard shard : shardList) {
				String shardIterator = null;
				GetShardIteratorRequest getShardIteratorRequest = new GetShardIteratorRequest();
				getShardIteratorRequest.setStreamName(streamName);
				getShardIteratorRequest.setShardId(shard.getShardId());
				getShardIteratorRequest
						.setShardIteratorType(KinesisEncryptionUtils.getProperties().getProperty("sharditerator_type"));
				GetShardIteratorResult getShardIteratorResult = kinesis.getShardIterator(getShardIteratorRequest);
				shardIterator = getShardIteratorResult.getShardIterator();
				// DecryptShardConsumerThread consumer = new
				// DecryptShardConsumerThread(shardIterator, shard.getShardId(),
				// kinesis, kms);
				DecryptShardConsumerThread consumer = new DecryptShardConsumerThread(shardIterator, shard.getShardId(),
						kinesis, context, keyArn);
				Thread consumerThread = new Thread(consumer);
				log.info("Starting thread to consumer for Shard :" + shard.getShardId() + "with ShardIterator :"
						+ shardIterator);
				consumerThread.start();
			}
		} catch (IOException ioe) {
			log.error("Unable to load properties file" + ioe.getMessage());
			throw new Exception(ioe.toString());
		}

	}

}
